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

#if !defined(AFX_RINGBUFFER_H__34632DCE_D620_4F7D_AEE8_4640F0B7F46F__INCLUDED_)
#define AFX_RINGBUFFER_H__34632DCE_D620_4F7D_AEE8_4640F0B7F46F__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CRingBuffer  
{
public:
	LPCTSTR GetText() const;
	void AddChar(TCHAR ch);
	CRingBuffer();
	virtual ~CRingBuffer();

private:
	int m_lines;
	void Scroll();
	int m_size;
	int m_pos;
	TCHAR m_text[160];
};

#endif // !defined(AFX_RINGBUFFER_H__34632DCE_D620_4F7D_AEE8_4640F0B7F46F__INCLUDED_)
