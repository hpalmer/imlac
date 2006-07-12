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

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CRingBuffer::CRingBuffer() :
	m_pos(0), m_lines(0)
{
	m_size = (sizeof(m_text) / sizeof(TCHAR)) - 1;
	m_text[0] = 0;
}

CRingBuffer::~CRingBuffer()
{

}

void CRingBuffer::AddChar(TCHAR ch)
{
	if (ch == 010)
	{
		// Handle backspace
		if (m_pos > 0)
		{
			int n = m_pos - 1;
			if (m_text[n] == 012)
			{
				n = n - 1;
				if ((n < 0) || (m_text[n] != 015))
				{
					n = n + 1;
				}
				if (--m_lines < 0)
				{
					m_lines = 0;
				}
			}
			m_text[n] = 0;
			m_pos = n;
		}
	}
	else if (ch == 014)
	{
		// Form feed (Ctrl-L) clears the buffer
		m_pos = 0;
		m_lines = 0;
		m_text[0] = 0;
	}
	else
	{
		// If the buffer is full, delete the first line
		if (m_pos >= m_size)
		{
			Scroll();
		}	
		m_text[m_pos++] = ch;
		m_text[m_pos] = 0;
		if (ch == 015)
		{
			AddChar(012);
			if (++m_lines >= 4)
			{
				Scroll();
			}
		}
	}
}

LPCTSTR CRingBuffer::GetText() const
{
	return m_text;
}

void CRingBuffer::Scroll()
{
	for (int i = 0; i < m_size; ++i)
	{
		if (m_text[i] == _T('\n'))
		{
			// Move the remaining text to the beginning of the buffer
			for (int j = i + 1; j <= m_pos; ++j)
			{
				m_text[j - (i + 1)] = m_text[j];
			}
			m_pos = m_pos - (i + 1);
			--m_lines;
			break;
		}
	}
	// If it was all one long line, delete it all
	if (m_pos >= m_size)
	{
		m_pos = 0;
		m_lines = 0;
	}
	
}
