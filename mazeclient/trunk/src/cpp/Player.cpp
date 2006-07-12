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
#include "Player.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CPlayer::CPlayer(CMaze *pMaze)
	: m_id(0), m_dstat(INACTIVE), m_dir(NORTH), m_dx(0), m_dy(0), m_score(0),
	  m_bullets(0), m_firedx(0), m_firedy(0), m_firedir(NORTH), m_deadcnt(0),
	  m_peeking(NOPEEK), m_pMaze(pMaze)
{
	for (int i = 0; i < sizeof(m_name); ++i)
	{
		m_name[i] = ' ';
	}
}

CPlayer::~CPlayer()
{

}

BOOL
CPlayer::isPlaying() const
{
	return (m_dstat == INACTIVE) ? FALSE : TRUE;
}

BOOL
CPlayer::isDying() const
{
	return (m_dstat == DYING) ? TRUE : FALSE;
}

void
CPlayer::SetPosition(int x, int y, MazeDir dir)
{
	m_dx = x;
	m_dy = y;
	m_dir = dir;
}

void
CPlayer::GetPosition(int &x, int &y, MazeDir &dir) const
{
	x = m_dx;
	y = m_dy;
	dir = m_dir;
}

MazeDir
CPlayer::GetDirection() const
{
	return m_dir;
}

MazeDir
CPlayer::TurnLeft()
{
	MazeDir dir = (MazeDir)((m_dir + 3) & 3);
	m_dir = dir;
	return dir;
}

MazeDir
CPlayer::TurnRight()
{
	MazeDir dir = (MazeDir)((m_dir + 1) & 3);
	m_dir = dir;
	return dir;
}

BOOL
CPlayer::MoveForward()
{
	BOOL result = FALSE;
	int xnew = m_dx;
	int ynew = m_dy;

	if ((m_dir == NORTH) || (m_dir == SOUTH))
	{
		ynew += ((m_dir == NORTH) ? -1 : 1);
	}
	else
	{
		xnew += ((m_dir == WEST) ? -1 : 1);
	}


	if (!m_pMaze->isWallAt(xnew, ynew))
	{
		m_dx = xnew;
		m_dy = ynew;
		result = TRUE;
	}

	return result;
}

BOOL
CPlayer::MoveBackward()
{
	BOOL result = FALSE;
	int xnew = m_dx;
	int ynew = m_dy;

	if ((m_dir == NORTH) || (m_dir == SOUTH))
	{
		ynew += ((m_dir == SOUTH) ? -1 : 1);
	}
	else
	{
		xnew += ((m_dir == EAST) ? -1 : 1);
	}


	if (!m_pMaze->isWallAt(xnew, ynew))
	{
		m_dx = xnew;
		m_dy = ynew;
		result = TRUE;
	}

	return result;
}

UINT CPlayer::GetId() const
{
	return m_id;
}

void CPlayer::Exit()
{

}

BOOL CPlayer::SameName(const char *name) const
{
	BOOL result = TRUE;

	for (int i = 0; i < 6; ++i)
	{
		if (m_name[i] != name[i])
		{
			result = FALSE;
			break;
		}
	}
	return result;
}

void CPlayer::SetId(UINT id)
{
	m_id = id;
	m_dstat = ACTIVE;
}

void CPlayer::SetName(const char *name)
{
	for (int i = 0; i < 6; ++i)
	{
		m_name[i] = name[i];
	}
	m_name[6] = 0;
}

void CPlayer::SetScore(int score)
{
	m_score = score;
}

void CPlayer::SetDeaths(int deaths)
{
	m_deadcnt = deaths;
}

void CPlayer::SetStatus(PlayStatus status)
{
	m_dstat = status;
}

BOOL CPlayer::PeekRight()
{
	BOOL result = FALSE;

	if (m_peeking == NOPEEK)
	{
		if (MoveForward())
		{
			TurnRight();
			m_peeking = PEEKRIGHT;
			result = TRUE;
		}
	}
	return result;
}

BOOL CPlayer::UnPeekRight()
{
	BOOL result = FALSE;

	if (m_peeking == PEEKRIGHT)
	{
		TurnLeft();
		if (MoveBackward())
		{
			m_peeking = NOPEEK;
			result = TRUE;
		}
		else
		{
			// Can't unpeek, just punt
			TurnRight();
			m_peeking = NOPEEK;
			result = TRUE;
		}
	}
	return result;
}

BOOL CPlayer::PeekLeft()
{
	BOOL result = FALSE;

	if (m_peeking == NOPEEK)
	{
		if (MoveForward())
		{
			TurnLeft();
			m_peeking = PEEKLEFT;
			result = TRUE;
		}
	}
	return result;
}

BOOL CPlayer::UnPeekLeft()
{
	BOOL result = FALSE;

	if (m_peeking == PEEKLEFT)
	{
		TurnRight();
		if (MoveBackward())
		{
			m_peeking = NOPEEK;
			result = TRUE;
		}
		else
		{
			// Can't unpeek, just punt
			TurnLeft();
			m_peeking = NOPEEK;
			result = TRUE;
		}
	}

	return result;
}

char * CPlayer::GetName()
{
	return m_name;
}

void CPlayer::SaveFirePosition()
{
	m_firedir = m_dir;
	m_firedx = m_dx;
	m_firedy = m_dy;
}

void CPlayer::GetFirePosition(int &dx, int &dy, MazeDir &dir) const
{
	dx = m_firedx;
	dy = m_firedy;
	dir = m_firedir;
}

BOOL CPlayer::isActive() const
{
	return (m_dstat == ACTIVE);
}

BOOL CPlayer::isInactive() const
{
	return (m_dstat == INACTIVE);
}

int CPlayer::GetScore() const
{
	return m_score;
}

int CPlayer::GetDeaths() const
{
	return m_deadcnt;
}

int CPlayer::BumpScore()
{
	return ++m_score;
}

int CPlayer::BumpDeaths()
{
	return ++m_deadcnt;
}
