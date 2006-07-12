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

static const UINT imlacWalls[] = {
	511,
	450,
	358,
	281,
	225,
	184,
	155,
	133,
	116,
	103,
	92,
	83,
	75,
	70,
	64,
	60,
	56,
	53,
	50,
	47,
	45,
	43,
	41,
	39,
	37,
	35,
	33,
	31,
	29,
	27,
	25
};

static TCHAR * dirChars[] = {_T("n"), _T("e"), _T("s"), _T("w")};
static TCHAR * dirUpper[] = {_T("N"), _T("E"), _T("S"), _T("W")};

// Heights for the Imlac font at scales: 1/2, 1, 2, 3
static int fontHeights[] = { -11, -21, -42, -63 };

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CDisplay::CDisplay(HWND hWnd, CMaze * pMaze) :
	m_hWnd(hWnd), m_pMaze(pMaze),
	m_backDC(NULL), m_origBitmap(NULL),
	m_origPen(NULL), m_origBrush(NULL),
	m_origFont(NULL), m_pRingBuffer(NULL),
	m_viewPlayer(NULL), m_topView(FALSE), m_showAll(FALSE)
{
	UseImlacPerspective();


	// Create a font for each of the Imlac scales: 1/2, 1, 2, 3
	LOGFONT lf;

	lf.lfHeight = 0;
	lf.lfWidth = 0;
	lf.lfEscapement = 0;
	lf.lfOrientation = 0;
	lf.lfWeight = FW_NORMAL;
	lf.lfItalic = FALSE;
	lf.lfUnderline = FALSE;
	lf.lfStrikeOut = FALSE;
	lf.lfCharSet = DEFAULT_CHARSET;
	lf.lfOutPrecision = OUT_TT_PRECIS;
	lf.lfClipPrecision = CLIP_DEFAULT_PRECIS;
	lf.lfQuality = ANTIALIASED_QUALITY;
	lf.lfPitchAndFamily = FIXED_PITCH;
	_tcscpy_s(lf.lfFaceName, LF_FACESIZE, _T(""));

	for (int i = 0; i < 4; ++i)
	{
		lf.lfHeight = fontHeights[i];
		m_textFont[i] = CreateFontIndirect(&lf);
	}
}

CDisplay::~CDisplay()
{
	DeleteBackDC();
	for (int i = 0; i < 4; ++i)
	{
		if (m_textFont[i] != NULL)
		{
			DeleteObject(m_textFont[i]);
		}
	}
}

void CDisplay::DeleteBackDC()
{
	// Delete any previous backing device context
	if (m_backDC != NULL)
	{
		if (m_origBitmap != NULL)
		{
			HBITMAP myBitmap = (HBITMAP) SelectObject(m_backDC, m_origBitmap);
			DeleteObject(myBitmap);
			m_origBitmap = NULL;
		}
		if (m_origPen != NULL)
		{
			HPEN myPen = (HPEN) SelectObject(m_backDC, m_origPen);
			DeleteObject(myPen);
			m_origPen = NULL;
		}
		if (m_origBrush != NULL)
		{
			HBRUSH myBrush = (HBRUSH) SelectObject(m_backDC, m_origBrush);
			DeleteObject(myBrush);
			m_origBrush = NULL;
		}
		if (m_origFont != NULL)
		{
			SelectObject(m_backDC, m_origFont);
			m_origFont = NULL;
		}
		DeleteDC(m_backDC);
		m_backDC = NULL;
	}
}

void CDisplay::Resize()
{
	HDC hdc = GetDC(m_hWnd);
	RECT rt;
	
	GetClientRect(m_hWnd, &rt);

	// Establish Imlac coordinate system
	SetMapMode(hdc, MM_ISOTROPIC);
	POINT prevOrg;
	SetWindowExtEx(hdc, 1023, 1023, NULL);
	SetViewportOrgEx(hdc, 0, rt.bottom, &prevOrg);
	SetViewportExtEx(hdc, rt.right, -rt.bottom, NULL);

	// Delete current backing device context if any
	DeleteBackDC();

	m_backDC = CreateCompatibleDC(hdc);
	HBITMAP bitmap = CreateCompatibleBitmap(hdc, rt.right - rt.left, rt.bottom - rt.top);
	m_origBitmap = (HBITMAP) SelectObject(m_backDC, bitmap);

	SetMapMode(m_backDC, MM_ISOTROPIC);
	SetWindowExtEx(m_backDC, 1023, 1023, NULL);
	SetViewportOrgEx(m_backDC, 0, rt.bottom, &prevOrg);
	SetViewportExtEx(m_backDC, rt.right, -rt.bottom, NULL);

	// Create pen and brush to draw in Imlac green (on black background)
	HPEN imlacPen = CreatePen(PS_SOLID, 1, RGB(0, 255, 0));
	HBRUSH imlacBrush = CreateSolidBrush(RGB(0, 255, 0));

	// Select them into the device context
	m_origPen = (HPEN) SelectObject(m_backDC, imlacPen);
	m_origBrush = (HBRUSH) SelectObject(m_backDC, imlacBrush);

	// Set default font to Imlac scale 1
	m_origFont = (HFONT) SelectObject(m_backDC, m_textFont[1]);

	SetTextColor(m_backDC, RGB(0, 255, 0));
	SetBkMode(m_backDC, TRANSPARENT);
}

// This function paints the player's view of the maze

void CDisplay::PaintPlayerView()
{
	PAINTSTRUCT ps;
	HDC hdc;
	int mwidth, mheight;

	hdc = m_backDC;

	mwidth = m_pMaze->GetWidth();
	mheight = m_pMaze->GetHeight();

	RECT rt;

	GetClientRect(m_hWnd, &rt);
	DPtoLP(hdc, (LPPOINT) &rt, 2);

	FillRect(hdc, &rt, (HBRUSH) GetStockObject(BLACK_BRUSH));

	if (m_topView)
	{
		PaintTopView(hdc, rt);
	}
	else
	{
		int dx, dy;
		MazeDir dir;
		int step;
		int windex;
		BOOL solid;
		BOOL nextbit;
		BOOL lastLeft, lastRight;

		m_viewPlayer->GetPosition(dx, dy, dir);

		if ((dir == NORTH) || (dir == SOUTH))
		{
			step = ((dir == NORTH) ? -1 : 1);

			for (windex = 0; windex < mheight; ++windex)
			{
				nextbit = m_pMaze->isWallAt(dx, dy + step);
				solid = m_pMaze->isWallAt(dx + step, dy);
				lastLeft = solid;
				DrawLeftWall(hdc, solid, nextbit, windex);

				solid = m_pMaze->isWallAt(dx - step, dy);
				lastRight = solid;
				DrawRightWall(hdc, solid, nextbit, windex);

				// We can see any players at this location
				if (windex > 0) DrawPlayersAt(hdc, dx, dy);

				if (nextbit)
					break;

				dy += step;
			}
		}
		else
		{
			step = ((dir == WEST) ? -1 : 1);

			for (windex = 0; windex < mwidth; ++windex)
			{
				nextbit = m_pMaze->isWallAt(dx + step, dy);
				solid = m_pMaze->isWallAt(dx, dy - step);
				lastLeft = solid;
				DrawLeftWall(hdc, solid, nextbit, windex);

				solid = m_pMaze->isWallAt(dx, dy + step);
				lastRight = solid;
				DrawRightWall(hdc, solid, nextbit, windex);

				// We can see any players at this location
				if (windex > 0) DrawPlayersAt(hdc, dx, dy);

				if (nextbit)
					break;

				dx += step;
			}
		}

		int x, y;
		const UINT * walls = m_walls;

		x = walls[0] + walls[windex+1];
		y = x;
		MoveToEx(hdc, x, y, NULL);

		x -= (walls[windex+1] << 1);
		LineTo(hdc, x, y);

		y -= (walls[windex+1] << 1);
		if (lastLeft)
		{
			LineTo(hdc, x, y);
		}
		else
		{
			MoveToEx(hdc, x, y, NULL);
		}

		x += (walls[windex+1] << 1);
		LineTo(hdc, x, y);

		if (lastRight)
		{
			y += (walls[windex+1] << 1);
			LineTo(hdc, x, y);
		}
	}

	PaintScores(hdc);

	PaintDirection(hdc);
	PaintRingBuffer(hdc);

	hdc = BeginPaint(m_hWnd, &ps);

	BitBlt(hdc, 0, 0, rt.right, rt.top, m_backDC, 0, 0, SRCCOPY);

	EndPaint(m_hWnd, &ps);
}

void
CDisplay::DrawLeftWall(HDC hdc, BOOL solid, BOOL nextbit, int windex) const
{
	const UINT * walls = m_walls;
	int x, y;

	if (!solid)
	{
		x = walls[0] - walls[windex];
		y = walls[0] + walls[windex];

		MoveToEx(hdc, x, y, NULL);

		if (windex != 0)
		{
			y -= (walls[windex] << 1);
			LineTo(hdc, x, y);
		}

		y = walls[0] - walls[windex+1];
		MoveToEx(hdc, x, y, NULL);

		x += (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);

		y += (walls[windex+1] << 1);
		if (nextbit)
		{
			MoveToEx(hdc, x, y, NULL);
		}
		else
		{
			LineTo(hdc, x, y);
		}

		x -= (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);
	}
	else
	{
		x = walls[0] - walls[windex+1];
		y = walls[0] + walls[windex+1];

		MoveToEx(hdc, x, y, NULL);

		x -= (walls[windex] - walls[windex+1]);
		y += (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);

		x = walls[0] - walls[windex];
		y = x;
		MoveToEx(hdc, x, y, NULL);

		x += (walls[windex] - walls[windex+1]);
		y = x;
		LineTo(hdc, x, y);
	}
}

void
CDisplay::DrawRightWall(HDC hdc, BOOL solid, BOOL nextbit, int windex) const
{
	const UINT * walls = m_walls;
	int x, y;

	if (!solid)
	{
		x = walls[0] + walls[windex];
		y = walls[0] + walls[windex];

		MoveToEx(hdc, x, y, NULL);

		if (windex != 0)
		{
			y -= (walls[windex] << 1);
			LineTo(hdc, x, y);
		}

		y = walls[0] - walls[windex+1];
		MoveToEx(hdc, x, y, NULL);

		x -= (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);

		y += (walls[windex+1] << 1);
		if (nextbit)
		{
			MoveToEx(hdc, x, y, NULL);
		}
		else
		{
			LineTo(hdc, x, y);
		}

		x += (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);
	}
	else
	{
		x = walls[0] + walls[windex+1];
		y = x;
		MoveToEx(hdc, x, y, NULL);

		x += (walls[windex] - walls[windex+1]);
		y = x;
		LineTo(hdc, x, y);

		x = walls[0] + walls[windex+1];
		y = walls[0] - walls[windex+1];
		MoveToEx(hdc, x, y, NULL);

		x += (walls[windex] - walls[windex+1]);
		y -= (walls[windex] - walls[windex+1]);
		LineTo(hdc, x, y);
	}
}

void CDisplay::DrawPlayersAt(HDC hdc, int x, int y)
{
	for (int id = 1; id < CMaze::MAXPLAYERS; ++id)
	{
		CPlayer * player = m_pMaze->GetPlayer(id);
		if ((player != NULL) && (player->isPlaying() || player->isDying()))
		{
			int px, py;
			MazeDir pdir;
			player->GetPosition(px, py, pdir);
			if ((px == x) && (py == y))
			{
				// We can see this one
				int vx, vy;
				MazeDir vdir;
				m_viewPlayer->GetPosition(vx, vy, vdir);

				// Either px == vx or py == vy, but we don't care which
				int distance = (px - vx) + (py - vy);
				if (distance < 0) distance = -distance;

				int dx = 511;
				int dy = m_walls[0] - m_walls[distance];

				MoveToEx(hdc, dx, dy, NULL);
				TextOut(hdc, dx - 30, dy, player->GetName(), 6);
				MoveToEx(hdc, dx, dy + 20, NULL);
				if (player->isDying())
				{
					MoveToEx(hdc, dx - 30, dy, NULL);
					LineTo(hdc, dx + 15, dy + 45);
					MoveToEx(hdc, dx + 15, dy, NULL);
					LineTo(hdc, dx - 30, dy + 45);
					MoveToEx(hdc, dx, dy + 20, NULL);
				}

				// Which way is he facing relative to me?
				if (vdir == pdir)
				{
					// Facing away from me
					LineTo(hdc, dx, dy + 35);
					LineTo(hdc, dx - 2, dy + 30);
					LineTo(hdc, dx + 2, dy + 30);
					LineTo(hdc, dx, dy + 35);
				}
				else if ((vdir ^ pdir) == 2)
				{
					// Facing toward me
					PatBlt(hdc, dx - 6, dy + 15, 3, 2, PATINVERT);
					PatBlt(hdc, dx + 3, dy + 15, 3, 2, PATINVERT);
				}
				else if (((vdir + 1) & 3) == pdir)
				{
					// Facing right
					LineTo(hdc, dx + 15, dy + 20);
					LineTo(hdc, dx + 10, dy + 22);
					LineTo(hdc, dx + 10, dy + 18);
					LineTo(hdc, dx + 15, dy + 20);
				}
				else
				{
					// assert(((pdir + 1) & 3) == vdir)
					// Facing left
					LineTo(hdc, dx - 15, dy + 20);
					LineTo(hdc, dx - 10, dy + 22);
					LineTo(hdc, dx - 10, dy + 18);
					LineTo(hdc, dx - 15, dy + 20);
				}
			}
		}
	}
}

void CDisplay::PaintScores(HDC hdc)
{
	int xpos = 054;
	int ypos = 01300;

	static int xoff[] = { 5, 013, 020, 026, 033, 041, 046 };

	for (int id = 1; id <= CMaze::MAXPLAYERS; ++id)
	{
		CPlayer * player = m_pMaze->GetPlayer(id);
		if (player != NULL)
		{
			const char * name = player->GetName();
			int kills = player->GetScore();
			int deaths = player->GetDeaths();
			int len = strlen(name);
			char score[16];
			while ((len > 0) && (name[len-1] == ' ')) --len;
			TextOut(hdc, xpos - xoff[len], ypos, name, len);
			sprintf_s(score, 16, "%d", player->GetScore());
			len = strlen(score);
			if (player == m_pMaze->GetLastShooter())
			{
				strcat_s(score, sizeof(score), "!");
			}
			TextOut(hdc, xpos - xoff[len], ypos - 040, score, strlen(score));
			sprintf_s(score, 16, "%d", player->GetDeaths());
			len = strlen(score);
			if (player == m_pMaze->GetLastVictim())
			{
				strcat_s(score, 16, "*");
			}
			TextOut(hdc, xpos - xoff[len], ypos - 0100, score, strlen(score));
			ypos -= 0200;
			if (ypos == 0300)
			{
				xpos = 01714;
				ypos = 01300;
			}
		}
	}
}

void CDisplay::PaintDirection(HDC hdc)
{
	DWORD err;
	HGDIOBJ font = SelectObject(hdc, m_textFont[3]);
	if (font == NULL)
	{
		err = GetLastError();
	}
	int n = (int) m_viewPlayer->GetDirection();
	err = TextOut(hdc, 01000, 01750, dirUpper[n], 1);
	if (err == 0) {
		err = GetLastError();
	}
	SelectObject(hdc, font);
}

void CDisplay::PaintTopView(HDC hdc, const RECT &rt)
{
	int mwidth, mheight;
	int xpos, ypos;
	int boxSide;

	mwidth = m_pMaze->GetWidth();
	mheight = m_pMaze->GetHeight();

	int top = rt.top - 80;
	boxSide = top / mheight;
	if (boxSide * mwidth > rt.right)
	{
		boxSide = rt.right / mwidth;
	}
	
	xpos = (rt.right - boxSide * mwidth) / 2;
	ypos = (top + boxSide * mheight) / 2;
	
	int vpx, vpy;
	MazeDir vpd;

	m_viewPlayer->GetPosition(vpx, vpy, vpd);

	for (int y = 0; y < mheight; ++y)
	{
		ypos -= boxSide;
		for (int x = 0; x < mwidth; ++x)
		{
			if (m_pMaze->isWallAt(x, y))
			{
				PatBlt(hdc, xpos, ypos, boxSide, boxSide, DSTINVERT);
			}
			else
			{
				if ((x == vpx) && (y == vpy))
				{
					TextOut(hdc, xpos, ypos + boxSide, dirChars[vpd], 1);
				}
				else if (m_showAll)
				{
					CPlayer * pPlayer = m_pMaze->GetPlayerAt(x, y);
				
					if (pPlayer != NULL)
					{
						TextOut(hdc, xpos, ypos + boxSide, dirChars[pPlayer->GetDirection()], 1);
					}
				}
			}
			xpos += boxSide;
		}
		xpos = (rt.right - boxSide * mwidth) / 2;
	}
	
}

void CDisplay::SetTopView(BOOL on)
{
	m_topView = on;
}

void CDisplay::SetTopViewAll(BOOL enable)
{
	m_showAll = enable;
}

BOOL CDisplay::IsTopView() const
{
	return m_topView;
}

void CDisplay::SetViewPlayer(CPlayer *player)
{
	m_viewPlayer = player;
}

void CDisplay::UseImlacPerspective()
{
	UseCustomPerspective(imlacWalls);
}

void CDisplay::UseCustomPerspective(const UINT *walls)
{
	for (int i = 0; i < CMaze::MAZEHEIGHT; ++i)
	{
		m_walls[i] = walls[i];
	}
}

void CDisplay::SetRingBuffer(CRingBuffer *pRingBuffer)
{
	m_pRingBuffer = pRingBuffer;
}

void CDisplay::PaintRingBuffer(HDC hdc)
{
	if (m_pRingBuffer != NULL)
	{
		LPCTSTR ptr = m_pRingBuffer->GetText();
		int ypos = 0130 + 21;
		int i = 0;

		// The size of the last text written will tell us where to
		// put the cursor.  Initialize the size by getting the
		// size of a single blank.
		SIZE tsize;
		GetTextExtentPoint32(hdc, _T(" "), 1, &tsize);
		tsize.cx = 0;

		while (ptr[i])
		{
			int start = i;
			int len = 0;
			while (ptr[i] && (ptr[i] != 015))
			{
				++i;
				++len;
			}

			if (len > 0)
			{
				TextOut(hdc, 0200, ypos, &ptr[start], len);
				GetTextExtentPoint32(hdc, &ptr[start], len, &tsize);
			}
			if (ptr[i] == 015)
			{
				if (ptr[++i] == 012)
				{
					++i;
				}
				ypos -= 21;
				tsize.cx = 0;
			}
		}

		// Draw the cursor
		int xpos = 0204 + tsize.cx;
		ypos -= tsize.cy - 2;
		MoveToEx(hdc, xpos, ypos, NULL);
		LineTo(hdc, xpos + 6, ypos);
	}
}