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
#include "resource.h"

static const int DEFAULT_SERVER_PORT = 8082;
static LPCTSTR DEFAULT_SERVER_NAMES[] = {
	_T("localhost"),
	_T("192.168.101.107"),
};

#define MAX_LOADSTRING 100

static const LPCTSTR szMazeWindowClass = _T("MazeWindowClass");

static const double sideSize = 10000.;

static const int BC_IGNORE = 0;
static const int BC_LEAVE = 1;
static const int BC_MOVE = 2;
static const int BC_KILL = 3;
static const int BC_NEW = 4;
static const int BC_ECHO = 5;
static const int BC_NEWRIGHT = 6;
static const int BC_NEWLEFT = 7;
static const int BC_NEWAROUND = 8;
static const int BC_NEWFWD = 9;
static const int BC_NEWBACK = 10;
	
static const char byteClass[] = {
		BC_IGNORE, BC_LEAVE, BC_MOVE, BC_KILL,									// 000-003
		BC_NEW, BC_IGNORE, BC_IGNORE, BC_IGNORE,								// 004-007
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 010-017
		BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT,						// 020-023
		BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT, BC_NEWRIGHT,						// 024-027
		BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT,							// 030-033
		BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT, BC_NEWLEFT,							// 034-037
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 040-047
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 050-057
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 060-067
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 070-077
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 100-107
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 110-117
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 120-127
		BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO, BC_ECHO,	// 130-137
		BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND,					// 140-143
		BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND, BC_NEWAROUND,					// 144-147
		BC_NEWFWD, BC_NEWFWD, BC_NEWFWD, BC_NEWFWD,								// 150-153
		BC_NEWFWD, BC_NEWFWD, BC_NEWFWD, BC_NEWFWD,								// 154-157
		BC_NEWBACK, BC_NEWBACK, BC_NEWBACK, BC_NEWBACK,							// 160-163
		BC_NEWBACK, BC_NEWBACK, BC_NEWBACK, BC_NEWBACK,							// 164-167
		BC_IGNORE, BC_IGNORE, BC_IGNORE, BC_IGNORE,								// 170-173
		BC_IGNORE, BC_IGNORE, BC_IGNORE, BC_IGNORE								// 174-177
};
	
// Global Variables:
HINSTANCE hInst;								// current instance
HWND g_mainWnd;
HWND g_mazeWnd;
HWND g_statusWnd;
BOOL g_resetNumlockOnExit = false;
int g_ignoreChar = -1;
CServer * g_pServer;
CMaze * g_pMaze;
CDisplay * g_pDisplay;
CPlayer * g_pPlayer;
CRingBuffer * g_pRingBuffer;
UINT g_serverPort;
TCHAR g_serverName[64];
TCHAR g_cmdLine[128];
BOOL g_useImlac = TRUE;
UINT g_focalDistance = 2500;
UINT g_wallDistance = 2500;
static UINT compWalls[32];
TCHAR szTitle[MAX_LOADSTRING];								// The title bar text
TCHAR szWindowClass[MAX_LOADSTRING];						// The title bar text

// Foward declarations of functions included in this code module:
ATOM				MyRegisterClass(HINSTANCE hInstance);
BOOL				InitInstance(HINSTANCE, int);
LRESULT CALLBACK	WndProc(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK	About(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK	Connect(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK	Perspect(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK	MazeWndProc(HWND, UINT, WPARAM, LPARAM);

static BOOL			OnMainWindowCreate(HWND hwnd, LPCREATESTRUCT lpCreateStruct);
static void			OnMainWindowSize(HWND hwnd, UINT state, int cx, int cy);
static void			OnPaint(HWND hWnd);
static void			OnKey(HWND hWnd, UINT vk, BOOL down, int repeat, UINT flags);
static void			OnMazeWindowSize(HWND hwnd, UINT state, int cx, int cy);
static void			DrawLeftWall(HDC hdc, BOOL solid, BOOL nextbit, int windex);
static void			DrawRightWall(HDC hdc, BOOL solid, BOOL nextbit, int windex);
static void			ProcessServerInput();
static void			ToggleNumlock();

int APIENTRY WinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPSTR     lpCmdLine,
                     int       nCmdShow)
{
	MSG msg;
	HACCEL hAccelTable;
	INITCOMMONCONTROLSEX icc = {
		sizeof(icc),
		ICC_BAR_CLASSES
	};

	if (!InitCommonControlsEx(&icc)) {
		DWORD err = GetLastError();
		return FALSE;
	}

	// Initialize global strings
	LoadString(hInstance, IDS_APP_TITLE, szTitle, MAX_LOADSTRING);
	LoadString(hInstance, IDC_MAZEWAR, szWindowClass, MAX_LOADSTRING);
	MyRegisterClass(hInstance);

	// Perform application initialization:
	if (!InitInstance (hInstance, nCmdShow)) 
	{
		return FALSE;
	}

	hAccelTable = LoadAccelerators(hInstance, (LPCTSTR)IDC_MAZEWAR);

	// Main message loop:

	BOOL quit = FALSE;

	while (!quit) {

		if (g_pServer != NULL)
			g_pServer->StartOutput();

		// Wait for a message in an alertable state so that async
		// socket I/O works.

		DWORD rc = MsgWaitForMultipleObjectsEx(0, NULL, INFINITE,
			QS_ALLINPUT, MWMO_ALERTABLE|MWMO_INPUTAVAILABLE);

		switch (rc) {
		case WAIT_OBJECT_0:

			// Dispatch all messages
			while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {


				if (msg.message == WM_QUIT)
				{
					quit = TRUE;
				}
				else if (!TranslateAccelerator(msg.hwnd, hAccelTable, &msg)) 
				{
					TranslateMessage(&msg);
					DispatchMessage(&msg);
				}
			}
			break;

		case WAIT_IO_COMPLETION:
			// Socket I/O completed.  Check for input from the maze server.
			if ((g_pServer != NULL) && g_pServer->HasInput()) {
				ProcessServerInput();
			}
			break;
		}
	}

	// Turn off NUMLOCK if it was off when we started
	if (g_resetNumlockOnExit) {
		ToggleNumlock();
	}

	return msg.wParam;
}



//
//  FUNCTION: MyRegisterClass()
//
//  PURPOSE: Registers the window class.
//
//  COMMENTS:
//
//    This function and its usage is only necessary if you want this code
//    to be compatible with Win32 systems prior to the 'RegisterClassEx'
//    function that was added to Windows 95. It is important to call this function
//    so that the application will get 'well formed' small icons associated
//    with it.
//
ATOM MyRegisterClass(HINSTANCE hInstance)
{
	WNDCLASSEX wcex;

	wcex.cbSize = sizeof(WNDCLASSEX); 

	wcex.style			= CS_OWNDC | CS_HREDRAW | CS_VREDRAW;
	wcex.lpfnWndProc	= (WNDPROC)WndProc;
	wcex.cbClsExtra		= 0;
	wcex.cbWndExtra		= 0;
	wcex.hInstance		= hInstance;
	wcex.hIcon			= LoadIcon(hInstance, (LPCTSTR)IDI_MAZEWAR);
	wcex.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wcex.hbrBackground	= (HBRUSH)(COLOR_WINDOW+1);
	wcex.lpszMenuName	= (LPCSTR)IDC_MAZEWAR;
	wcex.lpszClassName	= szWindowClass;
	wcex.hIconSm		= LoadIcon(wcex.hInstance, (LPCTSTR)IDI_SMALL);

	return RegisterClassEx(&wcex);
}

//
//   FUNCTION: InitInstance(HANDLE, int)
//
//   PURPOSE: Saves instance handle and creates main window
//
//   COMMENTS:
//
//        In this function, we save the instance handle in a global variable and
//        create and display the main program window.
//
BOOL
InitInstance(HINSTANCE hInstance, int nCmdShow)
{
	// Set up defaults
	g_pServer = NULL;
	_tcscpy_s(g_serverName, 64, DEFAULT_SERVER_NAMES[0]);
	g_serverPort = DEFAULT_SERVER_PORT;
	g_cmdLine[0] = 0;

	// Initialize Winsock
	WORD wVersion;
	WSADATA wsd;
	int err;

	wVersion = MAKEWORD(2, 2);
	err = WSAStartup(wVersion, &wsd);
	if (err == SOCKET_ERROR) {
		err = WSAGetLastError();
		return err;
	}

	// Register a class for the maze child window
	WNDCLASSEX wcex;
	wcex.cbSize = sizeof(WNDCLASSEX); 

	wcex.style			= CS_OWNDC | CS_HREDRAW | CS_VREDRAW;
	wcex.lpfnWndProc	= (WNDPROC)MazeWndProc;
	wcex.cbClsExtra		= 0;
	wcex.cbWndExtra		= 0;
	wcex.hInstance		= hInstance;
	wcex.hIcon			= 0;
	wcex.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wcex.hbrBackground	= (HBRUSH) GetStockObject(BLACK_BRUSH);
	wcex.lpszMenuName	= 0;
	wcex.lpszClassName	= szMazeWindowClass;
	wcex.hIconSm		= 0;

	RegisterClassEx(&wcex);

	hInst = hInstance; // Store instance handle in our global variable
	
	// Create the main window
	g_mainWnd = CreateWindow(szWindowClass, szTitle, WS_OVERLAPPEDWINDOW,
		CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL, hInstance, NULL);
	
	if (!g_mainWnd)
	{
		return FALSE;
	}

	ShowWindow(g_mainWnd, nCmdShow);
	UpdateWindow(g_mainWnd);
	
	return TRUE;
}

BOOL
OnMainWindowCreate(HWND hWnd, LPCREATESTRUCT lpCreateStruct)
{
	DWORD err;

	// Turn on NUMLOCK if it's not already on
	if ((GetKeyState(VK_NUMLOCK) & 1) == 0)
	{
		g_resetNumlockOnExit = true;
		ToggleNumlock();
	}

	// Create a status bar
	g_statusWnd = CreateWindowEx(0, STATUSCLASSNAME,
		NULL, WS_CHILD|WS_VISIBLE, CW_USEDEFAULT, CW_USEDEFAULT,
		CW_USEDEFAULT, CW_USEDEFAULT, hWnd, NULL, lpCreateStruct->hInstance, NULL);

	SendMessage(g_statusWnd, SB_SETTEXT, 0, (LPARAM) "Not Connected");

	err = GetLastError();

	g_mazeWnd = CreateWindowEx(WS_EX_CLIENTEDGE, szMazeWindowClass, _T("MazeWindow"),
		WS_CHILD|WS_VISIBLE, 0, 0, 10, 10,
		hWnd, 0, hInst, NULL);

	// Set up the maze with the default maze
	g_pMaze = new CMaze();
	g_pMaze->LoadDefaultMaze();

	// Initialize a player in the maze at a random location
	g_pPlayer = new CPlayer(g_pMaze);
	g_pMaze->SetRandomLocation(g_pPlayer);

	// Set up display of maze
	g_pDisplay = new CDisplay(g_mazeWnd, g_pMaze);
	g_pDisplay->SetViewPlayer(g_pPlayer);

	g_pRingBuffer = new CRingBuffer();
	g_pDisplay->SetRingBuffer(g_pRingBuffer);

	// Get everything sized
	SendMessage(hWnd, WM_SIZE, SIZE_RESTORED, MAKELONG(lpCreateStruct->cx, lpCreateStruct->cy));
	return TRUE;
}

void
OnMainWindowSize(HWND hWnd, UINT state, int cx, int cy)
{
	RECT rtcl;
	RECT rtsb;


	// Notify the status bar
	SendMessage(g_statusWnd, WM_SIZE, state, MAKELONG(cx, cy));

	// Figure out the largest square window that will fit in
	// the client area of the main window, minus that consumed
	// by the status bar.
	GetClientRect(hWnd, &rtcl);
	GetWindowRect(g_statusWnd, &rtsb);

	int width = rtcl.right - rtcl.left;
	int height = rtcl.bottom - (rtsb.bottom - rtsb.top);
	int side = (width < height) ? width : height;

	if (width != height)
	{
		// Constrain the main window to a size that keeps the
		// maze window square
		RECT rtmw;
		GetWindowRect(hWnd, &rtmw);
		if (width < height)
		{
			rtmw.bottom -= (height - width);
		}
		else
		{
			rtmw.right -= (width - height);
		}
		MoveWindow(hWnd, rtmw.left, rtmw.top, rtmw.right - rtmw.left,
			rtmw.bottom - rtmw.top, TRUE);
	}
	else
	{
		// This will notify the maze window
		MoveWindow(g_mazeWnd, 0, 0, side, side, FALSE);
	}
}

//
//  FUNCTION: WndProc(HWND, unsigned, WORD, LONG)
//
//  PURPOSE:  Processes messages for the main window.
//
//  WM_COMMAND	- process the application menu
//  WM_PAINT	- Paint the main window
//  WM_DESTROY	- post a quit message and return
//
//
LRESULT
CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	int wmId, wmEvent;

	switch (message) 
	{
		HANDLE_MSG(hWnd, WM_KEYDOWN, OnKey);
		HANDLE_MSG(hWnd, WM_KEYUP, OnKey);
		HANDLE_MSG(hWnd, WM_CREATE, OnMainWindowCreate);
		HANDLE_MSG(hWnd, WM_SIZE, OnMainWindowSize);

		case WM_CHAR:
			if (g_pServer != NULL)
			{
				int ch = wParam & 0177;

				// Ignore character previously handled by OnKey()
				if (ch == g_ignoreChar)
				{
					break;
				}
				if (byteClass[ch] != BC_ECHO)
				{
					if ((ch & 0100) != 0)
					{
						ch = ch & ~040;
						if (byteClass[ch] == BC_ECHO)
						{
							g_pServer->SendChar(ch);
						}
					}
				}
				else
				{
					g_pServer->SendChar(ch);
				}
			}
			break;

		case WM_COMMAND:
			wmId    = LOWORD(wParam); 
			wmEvent = HIWORD(wParam); 
			// Parse the menu selections:
			switch (wmId)
			{
			case IDM_CONNECT:
				DialogBox(hInst, (LPCTSTR)IDD_NCONNECT, hWnd, (DLGPROC)Connect);
				break;

			case IDM_VIEWALL:
				{
					HMENU hMenu = GetMenu(hWnd);
					int ret = GetMenuState(hMenu, IDM_VIEWALL, MF_BYCOMMAND);
					if (ret != -1)
					{
						ret = (ret & MF_CHECKED) ? MF_UNCHECKED : MF_CHECKED;
						CheckMenuItem(hMenu, IDM_VIEWALL, MF_BYCOMMAND|ret);
						g_pDisplay->SetTopViewAll(ret == MF_CHECKED);
					}
				}
				break;

			case IDM_PERSPECT:
				DialogBox(hInst, (LPCTSTR)IDD_DIALOG1, hWnd, (DLGPROC)Perspect);
				break;

			case IDM_ABOUT:
				DialogBox(hInst, (LPCTSTR)IDD_ABOUTBOX, hWnd, (DLGPROC)About);
				break;

			case IDM_EXIT:
				DestroyWindow(hWnd);
				break;

			default:
				return DefWindowProc(hWnd, message, wParam, lParam);
			}
			break;

		case WM_ERASEBKGND:
			break;

		case WM_DESTROY:
			if ((g_pPlayer != NULL) && g_pPlayer->isPlaying())
			{
				if (g_pServer != NULL)
				{
					g_pServer->Exit();
				}
			}
			PostQuitMessage(0);
			break;
		default:
			return DefWindowProc(hWnd, message, wParam, lParam);
   }
   return 0;
}

// Mesage handler for about box.
LRESULT
CALLBACK About(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	switch (message)
	{
		case WM_INITDIALOG:
				return TRUE;

		case WM_COMMAND:
			if (LOWORD(wParam) == IDOK || LOWORD(wParam) == IDCANCEL) 
			{
				EndDialog(hDlg, LOWORD(wParam));
				return TRUE;
			}
			break;
	}
    return FALSE;
}

LRESULT CALLBACK
Connect(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	int i, n;
	HWND hBox;
	switch (message)
	{
	case WM_INITDIALOG:
		SetDlgItemText(hDlg, IDC_HOSTBOX, g_serverName);
		SetDlgItemInt(hDlg, IDC_PORTBOX, g_serverPort, FALSE);
		SetDlgItemText(hDlg, IDC_CMDBOX, g_cmdLine);
		hBox = GetDlgItem(hDlg, IDC_HOSTBOX);
		for (i = 0; i < (sizeof(DEFAULT_SERVER_NAMES)/sizeof(LPCTSTR)); ++i)
		{
			n = SendMessage(hBox, CB_ADDSTRING, 0, (LPARAM) DEFAULT_SERVER_NAMES[i]);
		}
		return TRUE;

	case WM_COMMAND:
		if ((LOWORD(wParam) == IDOK) || (LOWORD(wParam) == IDCANCEL))
		{
			if (LOWORD(wParam) == IDOK)
			{
				HWND hCmd = GetDlgItem(hDlg, IDC_CMDBOX);
				UINT nlen;
				BOOL ok = FALSE;
				
				nlen = GetDlgItemText(hDlg, IDC_HOSTBOX, g_serverName,
					sizeof(g_serverName)/sizeof(TCHAR));
				g_serverPort = GetDlgItemInt(hDlg, IDC_PORTBOX, &ok, FALSE);
				
				nlen = GetDlgItemText(hDlg, IDC_CMDBOX, g_cmdLine,
					(sizeof(g_cmdLine)/sizeof(TCHAR)) - 1);
				g_cmdLine[nlen] = 0;
				
				if (g_pServer != NULL) {
					g_pServer->Exit();
				}
				
				g_pServer = new CServer(g_serverName, g_serverPort, g_cmdLine, g_pPlayer);
				if (g_pServer->Connect())
				{
					SendMessage(g_statusWnd, SB_SETTEXT, 0,
						(LPARAM) "Connected");
				}
				else
				{
					SendMessage(g_statusWnd, SB_SETTEXT, 0,
						(LPARAM) "Connection failed");
				}
				
			}
			EndDialog(hDlg, LOWORD(wParam));
			return TRUE;
		}
		break;
	}
	return FALSE;
}

LRESULT CALLBACK
Perspect(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
	HWND hChk = GetDlgItem(hDlg, IDC_USEIMLAC);

	switch (message)
	{
	case WM_INITDIALOG:
		SetDlgItemInt(hDlg, IDC_FEDIT, g_focalDistance, FALSE);
		SetDlgItemInt(hDlg, IDC_DEDIT, g_wallDistance, FALSE);
		Button_SetCheck(hChk, (g_useImlac) ? BST_CHECKED : BST_UNCHECKED);
		return TRUE;

	case WM_COMMAND:
		if ((LOWORD(wParam) == IDOK) || (LOWORD(wParam) == IDCANCEL))
		{
			if (LOWORD(wParam) == IDOK)
			{
				UINT fval, dval;
				BOOL ok;

				if ((Button_GetState(hChk) & 0x0003) == BST_UNCHECKED)
				{

					fval = GetDlgItemInt(hDlg, IDC_FEDIT, &ok, FALSE);
					if (ok)
						g_focalDistance = fval;
					
					dval = GetDlgItemInt(hDlg, IDC_DEDIT, &ok, FALSE);
					if (ok)
						g_wallDistance = dval;

					double k1 = sideSize * (double)g_focalDistance;
					double k2 = (double)g_focalDistance + (double)g_wallDistance;

					if (k2 == 0.0)
					{
						g_useImlac = TRUE;
						g_pDisplay->UseImlacPerspective();
					}
					else
					{
						double scale = 1.0;

						for (int i = 0; i < 32; ++i)
						{
							compWalls[i] = (int)((scale * k1) / (2.0 * (k2 + (double)i * sideSize)));
							scale = 511. / (double)compWalls[0];
						}

						compWalls[0] = 511;
						g_useImlac = FALSE;
						g_pDisplay->UseCustomPerspective(compWalls);
					}
				}
				else
				{
					g_useImlac = TRUE;
					g_pDisplay->UseImlacPerspective();
				}
				InvalidateRect(g_mainWnd, NULL, TRUE);
			}
			EndDialog(hDlg, LOWORD(wParam));
			return TRUE;
		}
		break;
	}

	return FALSE;
}

LRESULT
CALLBACK MazeWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	BOOL result = FALSE;

	switch (message) 
	{
		HANDLE_MSG(hWnd, WM_KEYDOWN, OnKey);
		HANDLE_MSG(hWnd, WM_KEYUP, OnKey);
		HANDLE_MSG(hWnd, WM_SIZE, OnMazeWindowSize);

		case WM_CHAR:
			{
				int ch = wParam & 0177;
				if (byteClass[ch] == BC_ECHO)
				{
					g_pServer->SendChar(ch);
				}
			}
			break;

		case WM_ERASEBKGND:
			break;

		case WM_PAINT:
			OnPaint(hWnd);
			break;

		case WM_TIMER:
			{
				CPlayer * target = g_pMaze->GetPlayer(wParam);
				if (target->isActive())
				{
					if (g_pMaze->CouldSee(g_pPlayer, target))
					{
						g_pServer->SendKill(g_pPlayer, target);
						g_pMaze->RecordKill(g_pPlayer, target);
						target->SetStatus(DYING);

						char shotMessage[128];
						sprintf_s(shotMessage, sizeof(shotMessage),
							"You shot %s\n", target->GetName());
						SendMessage(g_statusWnd, SB_SETTEXT, 0,
							(LPARAM) shotMessage);
					}
					else
					{
						KillTimer(hWnd, wParam);
					}
				}
				else
				{
					if (target->isDying())
					{
						target->SetStatus(ACTIVE);
					}
					KillTimer(hWnd, wParam);
				}
				InvalidateRect(hWnd, NULL, TRUE);
			}
			break;

		default:
			return DefWindowProc(hWnd, message, wParam, lParam);
   }
   return 0;
}

void
OnMazeWindowSize(HWND hWnd, UINT state, int cx, int cy)
{
	if (g_pDisplay != NULL)
	{
		g_pDisplay->Resize();
	}
}

void
OnKey(HWND hWnd, UINT vk, BOOL down, int repeat, UINT flags)
{
	BOOL moved = FALSE;
	int i;

	if (down)
	{
		switch (vk)
		{
		case VK_LEFT:
			for (i = 0; i < repeat; ++i)
			{
				g_pPlayer->TurnLeft();
				moved = TRUE;
			}
			if (g_pServer != NULL)
			{
				g_pServer->SendTurnLeft(g_pPlayer, repeat);
			}
			break;
			
		case VK_RIGHT:
			for (i = 0; i < repeat; ++i)
			{
				g_pPlayer->TurnRight();
				moved = TRUE;
			}
			if (g_pServer != NULL)
			{
				g_pServer->SendTurnRight(g_pPlayer, repeat);
			}
			break;
			
		case VK_UP:
			for (i = 0; i < repeat; ++i)
			{
				if (!g_pPlayer->MoveForward())
				{
					break;
				}
				moved = TRUE;
			}
			if (g_pServer != NULL)
			{
				g_pServer->SendMoveForward(g_pPlayer, i);
			}
			break;
			
		case VK_DOWN:
			for (i = 0; i < repeat; ++i)
			{
				if (!g_pPlayer->MoveBackward())
				{
					break;
				}
				moved = TRUE;
			}
			if (g_pServer != NULL)
			{
				g_pServer->SendMoveBackward(g_pPlayer, i);
			}
			break;
		
		case VK_NUMPAD0:
			if (g_pPlayer->PeekRight())
			{
				moved = TRUE;
				if (g_pServer != NULL)
				{
					g_pServer->SendPeekRight(g_pPlayer);
				}
			}
			g_ignoreChar = '0';
			break;

		case VK_NUMPAD1:
			if (g_pPlayer->PeekLeft())
			{
				moved = TRUE;
				if (g_pServer != NULL)
				{
					g_pServer->SendPeekLeft(g_pPlayer);
				}
			}
			g_ignoreChar = '1';
			break;

		case VK_NUMPAD4:
			for (i = 0; i < repeat; ++i)
			{
				g_pPlayer->TurnRight();
				g_pPlayer->TurnRight();
				moved = TRUE;
			}
			if (g_pServer != NULL)
			{
				g_pServer->SendTurnRight(g_pPlayer, 2*repeat);
			}
			g_ignoreChar = '4';
			break;
			
		case VK_NUMPAD7:
			if (!g_pDisplay->IsTopView())
			{
				g_pDisplay->SetTopView(TRUE);
				moved = TRUE;
			}
			g_ignoreChar = '7';
			break;

		case VK_ESCAPE:
			g_pMaze->Fire(g_mazeWnd, g_pPlayer);
			break;
		}
	}
	else
	{
		switch (vk)
		{
		case VK_NUMPAD0:
			if (g_pPlayer->UnPeekRight())
			{
				moved = TRUE;
				if (g_pServer != NULL)
				{
					g_pServer->SendUnPeekRight(g_pPlayer);
				}
			}
			g_ignoreChar = -1;
			break;

		case VK_NUMPAD1:
			if (g_pPlayer->UnPeekLeft())
			{
				moved = TRUE;
				if (g_pServer != NULL)
				{
					g_pServer->SendUnPeekLeft(g_pPlayer);
				}
			}
			// fall through
		case VK_NUMPAD4:
			g_ignoreChar = -1;
			break;

			
		case VK_NUMPAD7:
			g_pDisplay->SetTopView(FALSE);
			moved = TRUE;
			g_ignoreChar = -1;
			break;
		}
	}

	if (moved)
		InvalidateRect(hWnd, NULL, TRUE);
}

void
OnPaint(HWND hWnd)
{
	g_pDisplay->PaintPlayerView();
}


void
ProcessServerInput()
{
	int len = 0;
	int idmsg;
	MazeDir dir;
	int dx, dy;
	CPlayer * player;

	const char * ptr = g_pServer->GetInputPtr(&len);
	if (len <= 0)
		return;

	int bump = 0;
	int remaining = len;
	BOOL refresh = FALSE;

	// TODO:
	// When another player moves, if we can him, we need to redraw.
	// That includes turns.
	do {
		int b = ptr[0] & 0177;
		switch (byteClass[b]) {
		case BC_IGNORE:
			bump = 1;
			break;
		case BC_LEAVE:
			if (remaining >= 2) {
				idmsg = ptr[1];
				player = g_pMaze->GetPlayer(idmsg);
				if (player != NULL) {
					player->Exit();
					if (player == g_pPlayer) {
						player->SetStatus(INACTIVE);
						g_pServer->Exit();
						g_pServer = NULL;
					} else {
						g_pMaze->RemovePlayer(player);
						delete player;
					}
				}
				bump = 2;
				refresh = TRUE;
			}
			break;
		case BC_MOVE:
			if (remaining >= 5) {
				idmsg = ptr[1];
				dir = (MazeDir) (ptr[2] & 3);
				dx = ptr[3] & 077;
				dy = ptr[4] & 077;
				player = g_pMaze->GetPlayer(idmsg);
				if (player != NULL) {
					refresh |= g_pMaze->CanSee(g_pPlayer, player);
					player->SetPosition(dx, dy, dir);
					refresh |= g_pMaze->CanSee(g_pPlayer, player);
				}
				bump = 5;
			}
			break;
		case BC_KILL:
			if (remaining >= 3) {
				idmsg = ptr[1];
				int idother = ptr[2];
				player = g_pMaze->GetPlayer(idmsg);
				if (player != NULL) {
					CPlayer * victim = g_pMaze->GetPlayer(idother);
					g_pMaze->RecordKill(player, victim);
					if (victim != NULL)
					{
						if (victim == g_pPlayer)
						{
							char shotMessage[128];
							sprintf_s(shotMessage, sizeof(shotMessage), "You were shot by %s",
								player->GetName());
							SendMessage(g_statusWnd, SB_SETTEXT, 0,
								(LPARAM) shotMessage);
							g_pMaze->SetRandomLocation(g_pPlayer);
							g_pServer->SendPosition(g_pPlayer);
						}
					}
				}
				bump = 3;
				refresh = TRUE;
			}
			break;
		case BC_NEW:
			if (remaining >= 12) {
				idmsg = ptr[1];
				player = g_pMaze->GetPlayer(idmsg);
				if ((player != NULL) && player->SameName(ptr+2)) {
					player->Exit();
					if (player == g_pPlayer) {
						player->SetStatus(INACTIVE);
					} else {
						delete player;
					}
					player = NULL;
				}
				if (player == NULL) {
					if (g_pPlayer->GetId() == 0) {
						player = g_pPlayer;
					} else {
						player = new CPlayer(g_pMaze);
					}
					player->SetId(idmsg);
					player->SetName(ptr+2);
					int score = ((ptr[8] & 077) << 8) | (ptr[9] & 077);
					int deaths = ((ptr[10] & 077) << 8) | (ptr[11] & 077);
					player->SetScore(score);
					player->SetDeaths(deaths);
					player->SetStatus(ACTIVE);
					g_pMaze->AddPlayer(player);
					if (player == g_pPlayer)
						g_pServer->SendPosition(player);
				}
				bump = 12;
				refresh = TRUE;
			}
			break;
		case BC_ECHO:
			// Echo character to message buffer
			g_pRingBuffer->AddChar(ptr[0]);
			refresh = TRUE;
			bump = 1;
			break;
		case BC_NEWRIGHT:
			idmsg = (ptr[0] + 1) & 7;
			player = g_pMaze->GetPlayer(idmsg);
			if (player != NULL) {
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
				player->TurnRight();
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
			}
			bump = 1;
			break;
		case BC_NEWLEFT:
			idmsg = (ptr[0] + 1) & 7;
			player = g_pMaze->GetPlayer(idmsg);
			if (player != NULL) {
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
				player->TurnLeft();
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
			}
			bump = 1;
			break;
		case BC_NEWAROUND:
			idmsg = (ptr[0] + 1) & 7;
			player = g_pMaze->GetPlayer(idmsg);
			if (player != NULL) {
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
				player->TurnRight();
				player->TurnRight();
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
			}
			bump = 1;
			break;
		case BC_NEWFWD:
			idmsg = (ptr[0] + 1) & 7;
			player = g_pMaze->GetPlayer(idmsg);
			if (player != NULL) {
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
				player->MoveForward();
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
			}
			bump = 1;
			break;
		case BC_NEWBACK:
			idmsg = (ptr[0] + 1) & 7;
			player = g_pMaze->GetPlayer(idmsg);
			if (player != NULL) {
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
				player->MoveBackward();
				refresh |= g_pMaze->CanSee(g_pPlayer, player);
			}
			bump = 1;
			break;
		}
		ptr = (g_pServer != NULL) ? g_pServer->AdvanceInput(bump) : NULL;
	} while ((ptr != NULL) && (bump > 0));
	if (refresh || g_pDisplay->IsTopView())
		InvalidateRect(g_mazeWnd, NULL, TRUE);
}

void
ToggleNumlock()
{
	// Simulate a key press
	keybd_event( VK_NUMLOCK,
		0x45,
		KEYEVENTF_EXTENDEDKEY | 0,
		0 );

	// Simulate a key release
	keybd_event( VK_NUMLOCK,
		0x45,
		KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP,
		0);
}
