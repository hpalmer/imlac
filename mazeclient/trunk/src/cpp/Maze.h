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

#if !defined(AFX_MAZE_H__5D65B941_3B57_4E75_9799_2C553872A819__INCLUDED_)
#define AFX_MAZE_H__5D65B941_3B57_4E75_9799_2C553872A819__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CMaze  
{
public:
	CPlayer * GetLastShooter() const;
	CPlayer * GetLastVictim() const;
	void RecordKill(CPlayer * shooter, CPlayer * victim);
	BOOL CouldSee(const CPlayer * player, const CPlayer * other) const;
	int LineOfSight(int fromDx, int fromDy, MazeDir fromDir,
		int toDx, int toDy, MazeDir toDir) const;
	BOOL CanSee(const CPlayer * player, const CPlayer * other) const;
	void Fire(HWND hWnd, CPlayer * player);
	void SendTurnRight(int count);
	void SetRandomLocation(CPlayer * player);
	int AddPlayer(CPlayer * pPlayer);
	int GetHeight(void) const;
	CPlayer * GetPlayer(UINT id) const;
	CPlayer * GetPlayerAt(int x, int y) const;
	int GetWidth(void) const;
	BOOL isWallAt(int x, int y) const;
	void LoadDefaultMaze(void);
	void RemovePlayer(CPlayer * player);
	CMaze();
	virtual ~CMaze();

	enum {
		MAXPLAYERS=8,				// maximum number of players
		MAZEWIDTH=2,				// width of maze
		MAZEHEIGHT=32				// height of maze
	};

private:
	CPlayer * m_player[MAXPLAYERS];
	UCHAR m_maze[MAZEWIDTH*MAZEHEIGHT];
	CPlayer * m_lastVictim;
	CPlayer * m_lastShooter;
};

#endif // !defined(AFX_MAZE_H__5D65B941_3B57_4E75_9799_2C553872A819__INCLUDED_)
