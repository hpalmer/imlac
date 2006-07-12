/*
 * Copyright © 2004, 2005, 2006 by Howard Palmer.  All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

#include "stdafx.h"
#include <stdio.h>

static const int MAXRELMSG = 8;

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CServer::CServer(LPCTSTR serverName, UINT serverPort, LPCTSTR cmdLine,
				 CPlayer * player)
	: m_socket(0), m_inlen(0), m_inpos(0), m_outbtm(0), m_outtop(0),
	  m_relmsg(0), m_outpending(FALSE), m_exitFlag(FALSE),
	  m_player(player)
{
#ifdef _UNICODE
	WideCharToMultiByte(CP_ACP, WC_DEFAULTCHAR, serverName, -1,
		m_serverName, sizeof(m_serverName), "?", NULL);
	if (username != NULL) {
		WideCharToMultiByte(CP_ACP, WC_DEFAULTCHAR, cmdLine, -1,
			m_cmdLine, sizeof(m_cmdLine), "?", NULL);
#else
	strncpy_s(m_serverName, sizeof(m_serverName), serverName, sizeof(m_serverName));
	if (cmdLine != NULL) {
		strncpy_s(m_cmdLine, sizeof(m_cmdLine), cmdLine, sizeof(m_cmdLine));
	}
#endif

	m_serverPort = serverPort;
	m_recvOvlp.olp.Internal = 0;
	m_recvOvlp.olp.InternalHigh = 0;
	m_recvOvlp.olp.Offset = 0;
	m_recvOvlp.olp.OffsetHigh = 0;
	m_recvOvlp.olp.hEvent = 0;
	m_recvOvlp.pSvr = this;
	m_sendOvlp.olp.Internal = 0;
	m_sendOvlp.olp.InternalHigh = 0;
	m_sendOvlp.olp.Offset = 0;
	m_sendOvlp.olp.OffsetHigh = 0;
	m_sendOvlp.olp.hEvent = 0;
	m_sendOvlp.pSvr = this;
}

CServer::~CServer()
{
	if (m_socket != 0) {
		this->Close();
	}
}

void CServer::Exit()
{
	UINT id = m_player->GetId();
	if (id > 0) {
		char * ptr = GetOutputPtr(2);
		ptr[0] = 1;
		ptr[1] = id;
		AdvanceOutput(2);
		StartOutput();
		m_exitFlag = true;
	}
}

void CServer::Close()
{
	closesocket(m_socket);
	m_socket = 0;
}

BOOL CServer::Connect()
{
	BOOL result = FALSE;

	m_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (m_socket != INVALID_SOCKET) {
		int on = 1;
		int err = setsockopt(m_socket, IPPROTO_TCP, TCP_NODELAY, (const char *)&on, sizeof(on));
		if (err == SOCKET_ERROR) {
			err = WSAGetLastError();
		}
		addrinfo *aptr = NULL;
		addrinfo hints = {
			0,
			PF_INET,
			SOCK_STREAM,
			IPPROTO_TCP,
			0, NULL, NULL, NULL
		};
		char portBuf[8];
		sprintf_s(portBuf, sizeof(portBuf), "%d", m_serverPort);
		err = getaddrinfo(m_serverName, portBuf, &hints, &aptr);
		if (!err && (aptr != NULL)) {
			struct sockaddr *saptr = aptr->ai_addr;
			err = connect(m_socket, aptr->ai_addr, aptr->ai_addrlen);
			if (err) {
				err = WSAGetLastError();
			}
		}
		if (!err) {
			int len = strlen(m_cmdLine) + 1;
			char * ptr = GetOutputPtr(len);
			strcpy_s(ptr, sizeof(m_outbuf) - m_outtop, m_cmdLine);
			ptr[len-1] = '\n';
			AdvanceOutput(len);

			DWORD count = 0;
			DWORD flags = 0;
			WSABUF wsabuf;
			wsabuf.len = sizeof(m_inbuf);
			wsabuf.buf = m_inbuf;

			err = WSARecv(m_socket, &wsabuf, 1, &count, &flags,
				(LPWSAOVERLAPPED)&m_recvOvlp, ReceiveComplete);
			if ((err == 0) || ((err = WSAGetLastError()) == WSA_IO_PENDING)) {
				result = TRUE;
			} else {
				this->Close();
			}
		}
	}

	return result;
}

BOOL CServer::HasInput() const
{
	return (m_inpos < m_inlen);
}

void CALLBACK CServer::ReceiveComplete(DWORD dwError, DWORD cbTransferred,
									   LPWSAOVERLAPPED lpOverlapped, DWORD dwFlags)
{
	CServer * pSvr = ((struct MyOverlapped *)lpOverlapped)->pSvr;
	DWORD count = 0;
	DWORD flags = 0;
	BOOL result = WSAGetOverlappedResult(pSvr->m_socket, (LPWSAOVERLAPPED)&pSvr->m_recvOvlp,
		&count, FALSE, &flags);
	if (result) {
		if (count == 0)
		{
			// Server terminated connection
			pSvr->Close();
			return;
		}
		pSvr->m_inlen += count;

		// Copy remaining data to beginning of buffer before starting next read
		if (pSvr->m_inpos > 0) {
			count = pSvr->m_inlen - pSvr->m_inpos;
			if (count > 0) {
				memmove(pSvr->m_inbuf, pSvr->m_inbuf + pSvr->m_inpos, count);
			}
			pSvr->m_inlen = count;
			pSvr->m_inpos = 0;
		}
		WSABUF wsabuf;
		wsabuf.len = sizeof(pSvr->m_inbuf) - pSvr->m_inlen;
		wsabuf.buf = pSvr->m_inbuf + pSvr->m_inlen;
		count = 0;
		flags = 0;

		DWORD err = WSARecv(pSvr->m_socket, &wsabuf, 1, &count, &flags,
				(LPWSAOVERLAPPED)&pSvr->m_recvOvlp, ReceiveComplete);
		if ((err != 0) && ((err = WSAGetLastError()) != WSA_IO_PENDING)) {
			pSvr->Exit();
		}
	}
	else
	{
		DWORD err = WSAGetLastError();
		pSvr->Exit();
	}
}

void CALLBACK CServer::TransmitComplete(DWORD dwError, DWORD cbTransferred,
										LPWSAOVERLAPPED lpOverlapped, DWORD dwFlags)
{
	CServer * pSvr = ((struct MyOverlapped *)lpOverlapped)->pSvr;
	DWORD count = 0;
	DWORD flags = 0;
	BOOL result = WSAGetOverlappedResult(pSvr->m_socket, (LPWSAOVERLAPPED)&pSvr->m_sendOvlp,
		&count, FALSE, &flags);
	pSvr->m_outpending = FALSE;
	if (result) {
		pSvr->m_outbtm += count;
		pSvr->StartOutput();
		if (!pSvr->m_outpending && pSvr->m_exitFlag)
		{
			CPlayer * player = pSvr->m_player;
			player->SetId(0);
			player->SetStatus(INACTIVE);
			pSvr->Close();
			delete pSvr;
		}
	}
	else
	{
		DWORD err = WSAGetLastError();
		CPlayer * player = pSvr->m_player;
		player->SetId(0);
		player->SetStatus(INACTIVE);
		if (pSvr->m_exitFlag)
		{
			pSvr->Close();
			delete pSvr;
		}
	}
}

const char * CServer::GetInputPtr(int *len) const
{
	int avail = m_inlen - m_inpos;
	if (len != NULL) {
		*len = avail;
	}
	return (avail > 0) ? m_inbuf + m_inpos : NULL;
}

const char * CServer::AdvanceInput(int count)
{
	m_inpos += count;
	if (m_inpos > m_inlen) {
		m_inpos = m_inlen;
	}
	return (m_inlen - m_inpos) ? &m_inbuf[m_inpos] : NULL;
}

void CServer::SendTurnRight(const CPlayer * player, int count)
{
	count = count % 4;
	if (count > 0)
	{
		char * ptr;
		if ((m_relmsg + count) >= MAXRELMSG)
		{
			SendPosition(player);
		}
		else
		{
			ptr = GetOutputPtr(count);
			int cmd = ((player->GetId() - 1) & 7) | 020;

			for (int i = 0; i < count; ++i)
			{
				ptr[i] = cmd;
			}
			AdvanceOutput(count);
			m_relmsg += count;
		}
	}
}

void CServer::SendTurnLeft(const CPlayer * player, int count)
{
	count = count % 4;
	if (count > 0)
	{
		if ((m_relmsg + count) >= MAXRELMSG)
		{
			SendPosition(player);
		}
		else
		{
			char * ptr = GetOutputPtr(count);
			int cmd = ((player->GetId() - 1) & 7) | 030;

			for (int i = 0; i < count; ++i)
			{
				ptr[i] = cmd;
			}
			AdvanceOutput(count);
			m_relmsg += count;
		}
	}

}

void CServer::SendMoveForward(const CPlayer * player, int count)
{
	if (count > 0)
	{
		if ((count >= 5) || ((m_relmsg + count) >= MAXRELMSG))
		{
			SendPosition(player);
		}
		else
		{
			char * ptr = GetOutputPtr(count);
			int cmd = ((player->GetId() - 1) & 7) | 0150;

			for (int i = 0; i < count; ++i)
			{
				ptr[i] = cmd;
			}
			AdvanceOutput(count);
			m_relmsg += count;
		}
	}
}

void CServer::SendMoveBackward(const CPlayer * player, int count)
{
	if (count > 0)
	{
		if ((count >= 5) || ((m_relmsg + count) >= MAXRELMSG))
		{
			SendPosition(player);
		}
		else
		{
			char * ptr = GetOutputPtr(count);
			int cmd = ((player->GetId() - 1) & 7) | 0160;

			for (int i = 0; i < count; ++i)
			{
				ptr[i] = cmd;
			}
			AdvanceOutput(count);
			m_relmsg += count;
		}
	}
}

void CServer::SendPeekRight(const CPlayer * player)
{
	if ((m_relmsg + 2) >= MAXRELMSG)
	{
		SendPosition(player);
	}
	else
	{
		char * ptr = GetOutputPtr(2);
		int id = (player->GetId() - 1) & 7;
		ptr[0] = id | 0150;
		ptr[1] = id | 020;
		AdvanceOutput(2);
		m_relmsg += 2;
	}
}

void CServer::SendUnPeekRight(const CPlayer * player)
{
	if ((m_relmsg + 2) >= MAXRELMSG)
	{
		SendPosition(player);
	}
	else
	{
		char * ptr = GetOutputPtr(2);
		int id = (player->GetId() - 1) & 7;
		ptr[0] = id | 030;
		ptr[1] = id | 0160;
		AdvanceOutput(2);
		m_relmsg += 2;
	}
}

void CServer::SendPeekLeft(const CPlayer * player)
{
	if ((m_relmsg + 2) >= MAXRELMSG)
	{
		SendPosition(player);
	}
	else
	{
		char * ptr = GetOutputPtr(2);
		int id = (player->GetId() - 1) & 7;
		ptr[0] = id | 0150;
		ptr[1] = id | 030;
		AdvanceOutput(2);
		m_relmsg += 2;
	}

}

void CServer::SendUnPeekLeft(const CPlayer * player)
{
	if ((m_relmsg + 2) >= MAXRELMSG)
	{
		SendPosition(player);
	}
	else
	{
		char * ptr = GetOutputPtr(2);
		int id = (player->GetId() - 1) & 7;
		ptr[0] = id | 020;
		ptr[1] = id | 0160;
		AdvanceOutput(2);
		m_relmsg += 2;
	}
}

char * CServer::GetOutputPtr(int length)
{
	char * ptr = NULL;
	if ((m_outtop + length) <= sizeof(m_outbuf))
	{
		ptr = &m_outbuf[m_outtop];
	}
	return ptr;
}

char * CServer::AdvanceOutput(int length)
{
	char * ptr = NULL;
	if ((m_outtop + length) <= sizeof(m_outbuf))
	{
		m_outtop += length;
		ptr = &m_outbuf[m_outtop];
	}
	return ptr;
}

void CServer::SendPosition(const CPlayer * player)
{
	MazeDir dir;
	int dx, dy;
	player->GetPosition(dx, dy, dir);

	char * ptr;
	ptr = GetOutputPtr(5);
	ptr[0] = 2;
	ptr[1] = player->GetId();
	ptr[2] = (dir & 3) | 0100;
	ptr[3] = (dx & 077) | 0100;
	ptr[4] = (dy & 077) | 0100;
	AdvanceOutput(5);
	m_relmsg = 0;
}

void CServer::StartOutput()
{
	int count;

	// More to send?
	count = m_outtop - m_outbtm;
	if ((count > 0) && !m_outpending)
	{
		if (m_outbtm > 0)
		{
			memmove(m_outbuf, m_outbuf + m_outbtm, count);
			m_outbtm = 0;
			m_outtop = count;
		}
		WSABUF wsabuf;
		wsabuf.len = count;
		wsabuf.buf = m_outbuf;
		DWORD outcnt = 0;

		DWORD err = WSASend(m_socket, &wsabuf, 1, &outcnt, 0,
							(LPWSAOVERLAPPED)&m_sendOvlp, TransmitComplete);
		if ((err != 0) && ((err = WSAGetLastError()) != WSA_IO_PENDING)) {
			Close();
		}
		else
		{
			// The output may be finished, but the completion routine has not been called
			m_outpending = TRUE;
		}
	}
	else
	{
		m_outbtm = 0;
		m_outtop = 0;
	}
}

void CServer::SendKill(const CPlayer *killer, const CPlayer *victim)
{
	char * ptr = GetOutputPtr(3);
	if (ptr != NULL)
	{
		ptr[0] = 3;
		ptr[1] = killer->GetId();
		ptr[2] = victim->GetId();
		AdvanceOutput(3);
	}
}

void CServer::SendChar(int ch)
{
	char * ptr = GetOutputPtr(1);
	if (ptr != NULL)
	{
		ptr[0] = ch;
		AdvanceOutput(1);
	}
}
