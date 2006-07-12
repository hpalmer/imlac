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

//   The current default maze is:
//
//			      N O R T H
//
//
//	     $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
//	     $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
//	     $$$         $$$$$$   $$$                     $$$
//	     $$$    	 $$$$$$   $$$                     $$$
//	     $$$   $$$   $$$      $$$$$$   $$$$$$$$$$$$   $$$
//	     $$$   $$$   $$$      $$$$$$   $$$$$$$$$$$$   $$$
//	     $$$   $$$         $$$   $$$            $$$   $$$
//	     $$$   $$$         $$$   $$$            $$$   $$$
//	     $$$   $$$$$$   $$$            $$$   $$$$$$   $$$
//	     $$$   $$$$$$   $$$            $$$   $$$$$$   $$$
//	     $$$   $$$      $$$   $$$$$$$$$$$$            $$$
//	     $$$   $$$      $$$   $$$$$$$$$$$$            $$$
//	     $$$         $$$$$$   $$$            $$$$$$   $$$
//	     $$$         $$$$$$   $$$            $$$$$$   $$$
//	     $$$   $$$   $$$            $$$$$$$$$         $$$
//	     $$$   $$$   $$$            $$$$$$$$$         $$$
//	     $$$   $$$         $$$$$$               $$$   $$$
//	     $$$   $$$         $$$$$$               $$$   $$$
//	     $$$   $$$$$$$$$   $$$$$$$$$$$$$$$$$$$$$$$$   $$$
//	     $$$   $$$$$$$$$   $$$$$$$$$$$$$$$$$$$$$$$$   $$$
//	     $$$               $$$                  $$$   $$$
//	     $$$               $$$                  $$$   $$$
//	     $$$   $$$$$$$$$   $$$   $$$$$$$$$$$$   $$$   $$$
//	     $$$   $$$$$$$$$   $$$   $$$$$$$$$$$$   $$$   $$$
//	     $$$   $$$         $$$   $$$            $$$   $$$
//	     $$$   $$$         $$$   $$$            $$$   $$$
// 	W    $$$   $$$   $$$$$$$$$   $$$   $$$$$$         $$$     E
//	     $$$   $$$   $$$$$$$$$   $$$   $$$$$$         $$$
//	E    $$$   $$$               $$$            $$$   $$$     A
//	     $$$   $$$               $$$            $$$   $$$
//	S    $$$         $$$$$$   $$$$$$$$$$$$$$$   $$$   $$$     S
//	     $$$         $$$$$$   $$$$$$$$$$$$$$$   $$$   $$$
//	T    $$$   $$$   $$$      $$$               $$$   $$$     T
//	     $$$   $$$   $$$      $$$               $$$   $$$
//	     $$$$$$$$$   $$$$$$   $$$   $$$$$$$$$$$$$$$   $$$
//	     $$$$$$$$$   $$$$$$   $$$   $$$$$$$$$$$$$$$   $$$
//	     $$$   $$$      $$$                     $$$   $$$
//	     $$$   $$$      $$$                     $$$   $$$
//	     $$$         $$$$$$$$$$$$$$$$$$   $$$$$$$$$   $$$
//	     $$$         $$$$$$$$$$$$$$$$$$   $$$$$$$$$   $$$
//	     $$$   $$$                                    $$$
//	     $$$   $$$                                    $$$
//	     $$$   $$$$$$$$$   $$$$$$   $$$$$$$$$$$$$$$   $$$
//	     $$$   $$$$$$$$$   $$$$$$   $$$$$$$$$$$$$$$   $$$
//	     $$$         $$$   $$$                  $$$   $$$
//	     $$$         $$$   $$$                  $$$   $$$
//	     $$$   $$$   $$$   $$$   $$$$$$$$$$$$   $$$   $$$
//	     $$$   $$$   $$$   $$$   $$$$$$$$$$$$   $$$   $$$
//	     $$$   $$$   $$$   $$$   $$$      $$$   $$$   $$$
//	     $$$   $$$   $$$   $$$   $$$      $$$   $$$   $$$
//	     $$$   $$$         $$$   $$$   $$$$$$   $$$   $$$
//	     $$$   $$$         $$$   $$$   $$$$$$   $$$   $$$
//	     $$$         $$$   $$$                  $$$   $$$
//	     $$$         $$$   $$$                  $$$   $$$
//	     $$$   $$$$$$$$$   $$$   $$$$$$$$$$$$$$$$$$   $$$
//	     $$$   $$$$$$$$$   $$$   $$$$$$$$$$$$$$$$$$   $$$
//	     $$$                     $$$                  $$$
//	     $$$                     $$$                  $$$
//	     $$$   $$$$$$$$$   $$$$$$$$$   $$$$$$$$$$$$   $$$
//	     $$$   $$$$$$$$$   $$$$$$$$$   $$$$$$$$$$$$   $$$
//	     $$$      $$$                  $$$            $$$
//	     $$$      $$$                  $$$            $$$
//	     $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
//	     $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
//
//
//			       S O U T H
//

static const USHORT imlacDefault[] = {
	0177777,		// HERE IS THE 32 WORD MAZE.
	0106401,		// NO FOUR SQUARES MAY BE EMPTY.
	0124675,		// AND SHARE A COMMON CORNER.
	0121205,		// ALL OUTSIDE WALLS MUST BE FILLED IN.
	0132055,		// THIS IS THE DEFAULT MAZE.
	0122741,
	0106415,
	0124161,
	0121405,
	0135775,
	0101005,
	0135365,
	0121205,
	0127261,
	0120205,
	0106765,
	0124405,
	0166575,
	0122005,
	0107735,
	0120001,
	0135575,
	0105005,
	0125365,
	0125225,
	0121265,
	0105005,
	0135375,
	0100201,
	0135675,
	0110041,
	0177777
};

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CMaze::CMaze() :
	m_lastShooter(NULL), m_lastVictim(NULL)
{
	srand((unsigned) time(NULL));
	LoadDefaultMaze();

	for (int i = 0; i < MAXPLAYERS; ++i)
	{
		m_player[i] = NULL;
	}

}

CMaze::~CMaze()
{

}

BOOL
CMaze::isWallAt(int x, int y) const
{
	int i;
	int bx = x/8;

	if ((x < 0) || (y < 0) ||
		(bx >= MAZEWIDTH) || (y >= MAZEHEIGHT))
	{
		// Hmm..position out of range
		return TRUE;
	}

	i = y*MAZEWIDTH + bx;

	return (m_maze[i] & (1 << (x & 7))) ? TRUE : FALSE;
}


void
CMaze::LoadDefaultMaze()
{
	const USHORT * src = imlacDefault;
	int i;

	for (i = 0; i < sizeof(m_maze); ++i)
	{
		UCHAR sbits;
		UCHAR dbits;

		// Pick up high-order byte if i is even, low-order byte if odd
		sbits = (i & 1) ? (*src++ & 0xff) : (*src >> 8);

		// Reverse the bits
		dbits = 0;
		for (int j = 0; j < 8; ++j)
		{
			dbits = (dbits << 1) | (sbits & 1);
			sbits >>= 1;
		}

		m_maze[i] = dbits;
	}
}

int
CMaze::AddPlayer(CPlayer *pPlayer)
{
	int id = pPlayer->GetId();
	if ((id > 0) && (m_player[id-1] == NULL))
	{
		m_player[id-1] = pPlayer;
	}
	else
	{
		id = 0;
	}
	return id;
}

int
CMaze::GetWidth() const
{
	return MAZEWIDTH * 8;
}

int CMaze::GetHeight() const
{
	return MAZEHEIGHT;
}


CPlayer *
CMaze::GetPlayerAt(int x, int y) const
{
	int dx, dy;
	MazeDir dir;

	for (int i = 0; i < MAXPLAYERS; ++i)
	{
		if (m_player[i] != NULL)
		{
			m_player[i]->GetPosition(dx, dy, dir);
			if ((dx == x) && (dy == y))
			{
				return m_player[i];
			}
		}
	}

	return NULL;
}

void CMaze::RemovePlayer(CPlayer *player)
{
	int id = player->GetId();
	if ((id > 0) && (m_player[id-1] == player))
	{
		m_player[id-1] = NULL;
		player->SetStatus(INACTIVE);
	}
}

CPlayer * CMaze::GetPlayer(UINT id) const
{
	return m_player[id-1];
}



void CMaze::SetRandomLocation(CPlayer *player)
{
	int x, y;
	MazeDir dir;
	
	do
	{
		x = 1 + (rand() % (MAZEWIDTH * 8 - 2));
		y = 1 + (rand() % (MAZEHEIGHT - 2));
		dir = (MazeDir)((x ^ y) & 3);
	}
	while (isWallAt(x, y));
	player->SetPosition(x, y, dir);
}

void CMaze::Fire(HWND hWnd, CPlayer *player)
{
	player->SaveFirePosition();
	for (int i = 0; i < MAXPLAYERS; ++i)
	{
		CPlayer * target = m_player[i];
		if ((target != NULL) && CanSee(player, target))
		{
			int ret = SetTimer(hWnd, target->GetId(), 2000, NULL);
			if (ret == 0) ret = GetLastError();
		}
	}
}

BOOL CMaze::CanSee(const CPlayer *player, const CPlayer *other) const
{
	BOOL result = FALSE;
	int px, py;
	MazeDir pdir;

	player->GetPosition(px, py, pdir);

	if (player->isPlaying() && other->isPlaying())
	{
		int ox, oy;
		MazeDir odir;

		other->GetPosition(ox, oy, odir);
		result = (LineOfSight(px, py, pdir, ox, oy, odir) > 0);
	}
	return result;
}

int CMaze::LineOfSight(int fromDx, int fromDy, MazeDir fromDir,
					   int toDx, int toDy, MazeDir toDir) const
{
	UINT result = -1;
	int step;
	int dist;

	switch (fromDir)
	{
	case NORTH:
	case SOUTH:
		if (fromDx == toDx)
		{
			step = (fromDir == NORTH) ? -1 : 1;
			dist = (toDy - fromDy) * step;
			result = dist;
			if (dist > 0)
			{
				for (int y = fromDy; y != toDy; y += step)
				{
					if (isWallAt(fromDx, y))
					{
						dist = -1;
						break;
					}
				}
			}
		}
		break;

	case EAST:
	case WEST:
		if (fromDy == toDy)
		{
			step = (fromDir == WEST) ? -1 : 1;
			dist = (toDx - fromDx) * step;
			result = dist;
			if (dist > 0)
			{
				for (int x = fromDx; x != toDx; x += step)
				{
					if (isWallAt(x, fromDy))
					{
						dist = -1;
						break;
					}
				}
			}
		}
		break;
	}
	return result;
}

BOOL CMaze::CouldSee(const CPlayer *player, const CPlayer *other) const
{
	BOOL result = FALSE;
	int px, py;
	MazeDir pdir;

	if (player->isPlaying() && other->isPlaying())
	{
		player->GetFirePosition(px, py, pdir);

		int ox, oy;
		MazeDir odir;

		other->GetPosition(ox, oy, odir);
		result = (LineOfSight(px, py, pdir, ox, oy, odir) > 0);
	}
	return result;
}

void CMaze::RecordKill(CPlayer *shooter, CPlayer *victim)
{
	shooter->BumpScore();
	victim->BumpDeaths();
	m_lastShooter = shooter;
	m_lastVictim = victim;
}


CPlayer * CMaze::GetLastVictim() const
{
	return m_lastVictim;
}

CPlayer * CMaze::GetLastShooter() const
{
	return m_lastShooter;
}
