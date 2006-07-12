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

#if !defined(AFX_PLAYER_H__ECA07200_824A_42FC_9BBC_2BA8BC7A4CDB__INCLUDED_)
#define AFX_PLAYER_H__ECA07200_824A_42FC_9BBC_2BA8BC7A4CDB__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

typedef enum { INACTIVE, ACTIVE, DYING } PlayStatus;
typedef enum { NORTH=0, EAST=1, SOUTH=2, WEST=3 } MazeDir;
typedef enum { NOPEEK=0, PEEKLEFT=1, PEEKRIGHT=2 } Peek;

class CMaze;

class CPlayer  
{
public:
	int BumpDeaths();
	int BumpScore();
	int GetDeaths() const;
	int GetScore() const;
	BOOL isInactive() const;
	BOOL isActive() const;
	void GetFirePosition(int &dx, int &dy, MazeDir &dir) const;
	void SaveFirePosition();
	char * GetName();
	BOOL UnPeekLeft();
	BOOL PeekLeft();
	BOOL UnPeekRight();
	BOOL PeekRight();
	void SetStatus(PlayStatus status);
	void SetDeaths(int deaths);
	void SetScore(int score);
	void SetName(const char *name);
	void SetId(UINT id);
	void SetPosition(int x, int y, MazeDir dir);
	BOOL SameName(const char *name) const;
	void Die(UINT idKiller);
	void Hits(UINT idVictim);
	void Exit();
	UINT GetId() const;
	BOOL MoveBackward(void);
	BOOL MoveForward(void);
	MazeDir TurnRight(void);
	MazeDir TurnLeft(void);
	MazeDir GetDirection(void) const;
	void GetPosition(int &x, int &y, MazeDir &dir) const;
	BOOL isPlaying(void) const;
	BOOL isDying(void) const;
	CPlayer(CMaze *pMaze);
	virtual ~CPlayer();

private:
	CMaze * m_pMaze;
	char m_name[8];			// player name
	UINT m_id;				// player id
	PlayStatus m_dstat;		// status word
	MazeDir m_dir;			// direction
	int m_dx;				// x location
	int m_dy;				// y location
	int m_score;			// players score
	int m_bullets;			// bullet counter
	MazeDir m_firedir;		// direction at time of fire
	int m_firedx;			// x location at time of fire
	int m_firedy;			// y location at time of fire
	int m_deadcnt;			// shot dead counter

protected:
	Peek m_peeking;
};

#endif // !defined(AFX_PLAYER_H__ECA07200_824A_42FC_9BBC_2BA8BC7A4CDB__INCLUDED_)
