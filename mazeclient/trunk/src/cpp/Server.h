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

#if !defined(AFX_SERVER_H__5C5E62BF_03C7_4F56_BCEB_EA5AB94C91C0__INCLUDED_)
#define AFX_SERVER_H__5C5E62BF_03C7_4F56_BCEB_EA5AB94C91C0__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CPlayer;

class CServer  
{
public:
	void SendChar(int ch);
	void SendKill(const CPlayer * killer, const CPlayer * victim);
	void StartOutput();
	BOOL HasOutput() const;
	void SendPosition(const CPlayer * player);
	void SendUnPeekLeft(const CPlayer * player);
	void SendPeekLeft(const CPlayer * player);
	void SendUnPeekRight(const CPlayer * player);
	void SendPeekRight(const CPlayer * player);
	void SendMoveBackward(const CPlayer * player, int count);
	void SendMoveForward(const CPlayer * player, int count);
	void SendTurnLeft(const CPlayer * player, int count);
	void SendTurnRight(const CPlayer * player, int count);
	const char * AdvanceInput(int count);
	const char * GetInputPtr(int * len) const;
	BOOL HasInput() const;
//	int Send(char *buf, int len);
	BOOL Connect();
	void Exit();
	CServer(LPCTSTR serverName, UINT serverPort, LPCTSTR username,
		CPlayer * player);
	virtual ~CServer();

protected:
	void Close();
	char * AdvanceOutput(int length);
	char * GetOutputPtr(int length);

	static void CALLBACK ReceiveComplete(DWORD dwError, DWORD cbTransferred,
		LPWSAOVERLAPPED lpOverlapped, DWORD dwFlags);
	static void CALLBACK TransmitComplete(DWORD dwError, DWORD cbTransferred,
		LPWSAOVERLAPPED lpOverlapped, DWORD dwFlags);

	SOCKET m_socket;
	CPlayer * m_player;
	char m_serverName[64];
	char m_cmdLine[128];
	UINT m_serverPort;
	struct MyOverlapped {
		WSAOVERLAPPED olp;
		CServer * pSvr;
	} m_recvOvlp;
	struct MyOverlapped m_sendOvlp;
	int m_inlen;
	int m_inpos;
	char m_inbuf[256];
	char m_outbuf[256];
	int m_relmsg;				// count of relative moves sent
	int m_outbtm;
	int m_outtop;
	BOOL m_outpending;
	BOOL m_exitFlag;
	CPlayer * m_exitPlayer;
};

#endif // !defined(AFX_SERVER_H__5C5E62BF_03C7_4F56_BCEB_EA5AB94C91C0__INCLUDED_)
