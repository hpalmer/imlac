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

#if !defined(AFX_DISPLAY_H__9A273E71_F188_480F_A34C_58AF48F181A0__INCLUDED_)
#define AFX_DISPLAY_H__9A273E71_F188_480F_A34C_58AF48F181A0__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CDisplay  
{
public:
	void SetRingBuffer(CRingBuffer * pRingBuffer);
	void Resize();
	void PaintTopView(HDC hdc, const RECT &rt);
	void PaintDirection(HDC hdc);
	void PaintScores(HDC hdc);
	void PaintPlayerView();
	BOOL IsTopView() const;
	void SetTopView(BOOL on);
	void SetTopViewAll(BOOL enable);
	void SetViewPlayer(CPlayer * player);
	void UseCustomPerspective(const UINT * walls);
	void UseImlacPerspective();
	CDisplay(HWND hWnd, CMaze * pMaze);
	virtual ~CDisplay();

protected:
	void DrawPlayersAt(HDC hdc, int x, int y);
	void DrawLeftWall(HDC hdc, BOOL solid, BOOL nextbit, int windex) const;
	void DrawRightWall(HDC hdc, BOOL solid, BOOL nextbit, int windex) const;

private:
	void PaintRingBuffer(HDC hdc);
	CRingBuffer * m_pRingBuffer;
	void DeleteBackDC();
	HWND m_hWnd;
	CMaze * m_pMaze;
	HDC m_backDC;
	HBITMAP m_origBitmap;
	HPEN m_origPen;
	HBRUSH m_origBrush;
	UINT m_walls[CMaze::MAZEHEIGHT];
	BOOL m_topView;
	BOOL m_showAll;
	HFONT m_textFont[4];
	HFONT m_origFont;
	CPlayer * m_viewPlayer;

};

#endif // !defined(AFX_DISPLAY_H__9A273E71_F188_480F_A34C_58AF48F181A0__INCLUDED_)
