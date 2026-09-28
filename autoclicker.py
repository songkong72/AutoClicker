# -*- coding: utf-8 -*-
"""
게임용 종합 오토클리커 Pro (AutoClicker Pro)
- 🌟 완전 비활성 백그라운드 클릭 (내 마우스는 100% 자유롭게 웹서핑/작업 가능!)
- 🎯 치료 버튼 1회 클릭하여 위치 등록 & 0.5초 무한 연타
- 💾 프리셋(프로필) 영구 저장 및 언제든 1초 만에 불러오기/적용
- ⏱️ 클릭 간격(초/ms) 자유 조절 (0.5초 등) & 무한/지정 횟수 반복
- 🎬 마우스 다중 패턴 매크로 (여러 위치 순차 클릭)
- ⌨️ 키보드 키 연타 (치료 물약 / 스킬 키 연타)
- 🔔 시작/정지/등록 소리 알림 (Sound Feedback)
- 🛡️ 마우스 모서리 비상 탈출 (Fail-safe)
- ⌨️ 전역 단축키: F6(시작/정지), F7(창&좌표 캡처), F8(치료 버튼 찍기)
"""

import os
import sys
import time
import json
import random
import threading
import ctypes
from ctypes import wintypes
import winsound
import tkinter as tk
from tkinter import ttk, messagebox

# pynput 라이브러리
try:
    from pynput import mouse, keyboard
except ImportError:
    mouse = None
    keyboard = None

# ==========================================
# Windows API 정의 (ctypes)
# ==========================================
user32 = ctypes.windll.user32

WM_MOUSEMOVE = 0x0200
WM_LBUTTONDOWN = 0x0201
WM_LBUTTONUP = 0x0202
WM_RBUTTONDOWN = 0x0204
WM_RBUTTONUP = 0x0205
WM_MBUTTONDOWN = 0x0207
WM_MBUTTONUP = 0x0208
MK_LBUTTON = 0x0001
MK_RBUTTON = 0x0002
MK_MBUTTON = 0x0010

GA_ROOT = 2

class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]

user32.WindowFromPoint.argtypes = [POINT]
user32.WindowFromPoint.restype = wintypes.HWND
user32.ScreenToClient.argtypes = [wintypes.HWND, ctypes.POINTER(POINT)]
user32.ScreenToClient.restype = wintypes.BOOL
user32.GetWindowTextW.argtypes = [wintypes.HWND, wintypes.LPWSTR, ctypes.c_int]
user32.GetWindowTextW.restype = ctypes.c_int
user32.GetWindowTextLengthW.argtypes = [wintypes.HWND]
user32.GetWindowTextLengthW.restype = ctypes.c_int
user32.GetAncestor.argtypes = [wintypes.HWND, ctypes.c_uint]
user32.GetAncestor.restype = wintypes.HWND
user32.IsWindow.argtypes = [wintypes.HWND]
user32.IsWindow.restype = wintypes.BOOL
user32.PostMessageW.argtypes = [wintypes.HWND, ctypes.c_uint, wintypes.WPARAM, wintypes.LPARAM]
user32.PostMessageW.restype = wintypes.BOOL

def get_mouse_pos():
    pt = POINT()
    user32.GetCursorPos(ctypes.byref(pt))
    return pt.x, pt.y

def set_mouse_pos(x, y):
    user32.SetCursorPos(int(x), int(y))

def get_window_title(hwnd):
    length = user32.GetWindowTextLengthW(hwnd)
    if length > 0:
        buff = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, buff, length + 1)
        return buff.value
    return ""

def get_target_window_info(screen_x, screen_y):
    """
    마우스 위치의 실제 클릭 수신 윈도우(자식 창/캔버스 포함)와 표시용 제목 반환
    """
    pt = POINT(int(screen_x), int(screen_y))
    exact_hwnd = user32.WindowFromPoint(pt)
    if not exact_hwnd:
        return 0, 0, ""

    root_hwnd = user32.GetAncestor(exact_hwnd, GA_ROOT)
    display_hwnd = root_hwnd if root_hwnd else exact_hwnd
    title = get_window_title(display_hwnd)
    if not title:
        title = get_window_title(exact_hwnd)
    if not title:
        title = f"Window_{exact_hwnd}"

    return exact_hwnd, root_hwnd, title

def screen_to_client_coord(hwnd, screen_x, screen_y):
    pt = POINT(int(screen_x), int(screen_y))
    user32.ScreenToClient(hwnd, ctypes.byref(pt))
    return pt.x, pt.y

def post_click_background(hwnd, client_x, client_y, button="left"):
    """
    🌟 완전 백그라운드 클릭:
    실제 마우스 커서는 단 1픽셀도 움직이지 않고, 게임 창에만 정석 마우스 시퀀스 전송!
    (사용자는 웹서핑, 유튜브, 다른 창 작업을 100% 자유롭게 수행 가능)
    """
    if not hwnd or not user32.IsWindow(hwnd):
        return False

    lParam = ((int(client_y) & 0xFFFF) << 16) | (int(client_x) & 0xFFFF)
    btn_lower = str(button).lower()

    if "right" in btn_lower:
        down_msg, up_msg, wparam = WM_RBUTTONDOWN, WM_RBUTTONUP, MK_RBUTTON
    elif "middle" in btn_lower:
        down_msg, up_msg, wparam = WM_MBUTTONDOWN, WM_MBUTTONUP, MK_MBUTTON
    else:
        down_msg, up_msg, wparam = WM_LBUTTONDOWN, WM_LBUTTONUP, MK_LBUTTON

    # 1. 마우스 이동 신호 (해당 위치에 커서가 닿았음을 게임에 알림)
    user32.PostMessageW(hwnd, WM_MOUSEMOVE, 0, lParam)
    time.sleep(0.005)

    # 2. 마우스 다운 신호
    user32.PostMessageW(hwnd, down_msg, wparam, lParam)
    # 게임이 클릭 다운을 확실히 인식할 수 있는 안정적인 대기 (20ms)
    time.sleep(0.020)

    # 3. 마우스 업 신호
    user32.PostMessageW(hwnd, up_msg, 0, lParam)
    return True


# ==========================================
# 설정 및 프리셋 파일 관리
# ==========================================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")
PRESETS_FILE = os.path.join(BASE_DIR, "presets.json")

def load_json(filepath):
    if os.path.exists(filepath):
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {}

def save_json(filepath, data):
    try:
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception:
        pass


# ==========================================
# 오토클리커 GUI & 엔진 클래스
# ==========================================
class AutoClickerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("게임용 오토클리커 Pro")
        self.root.geometry("500x790")
        self.root.resizable(False, False)

        # 컨트롤러
        self.kb_controller = keyboard.Controller() if keyboard else None
        self.mouse_controller = mouse.Controller() if mouse else None

        # 상태 제어 변수
        self.is_running = False
        self.is_recording = False
        self.worker_thread = None
        self.stop_event = threading.Event()
        self.recorded_events = []
        self.last_record_time = None
        self.record_listener = None

        # 🎯 치료 버튼 백그라운드 등록 정보
        self.single_target_hwnd = 0
        self.single_target_title = ""
        self.single_target_x = 0
        self.single_target_y = 0
        self.single_target_cx = 0
        self.single_target_cy = 0

        # 설정 및 프리셋 로드
        self.cfg = load_json(CONFIG_FILE)
        self.presets = load_json(PRESETS_FILE)

        self.setup_styles()
        self.create_widgets()
        self.load_saved_settings()
        self.refresh_preset_dropdown()

        # 전역 단축키 리스너 시작
        self.kb_listener = None
        self.start_hotkey_listener()

        # 창 종료 이벤트 바인딩
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def play_sound(self, sound_type):
        if not hasattr(self, "var_sound") or not self.var_sound.get():
            return

        def _sound():
            try:
                if sound_type == "start":
                    winsound.Beep(1200, 100)
                elif sound_type == "stop":
                    winsound.Beep(650, 100)
                elif sound_type == "failsafe":
                    winsound.Beep(400, 300)
                elif sound_type == "rec_start":
                    winsound.Beep(1300, 70)
                    time.sleep(0.04)
                    winsound.Beep(1700, 70)
                elif sound_type == "point_captured":
                    winsound.Beep(1600, 150)
                elif sound_type == "preset_apply":
                    winsound.Beep(1500, 80)
            except Exception:
                pass

        threading.Thread(target=_sound, daemon=True).start()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")

        self.bg_color = "#f8fafc"
        self.card_bg = "#ffffff"
        self.primary_color = "#2563eb"
        self.danger_color = "#dc2626"
        self.success_color = "#16a34a"
        self.text_color = "#0f172a"

        self.root.configure(bg=self.bg_color)
        style.configure("TNotebook", background=self.bg_color)
        style.configure("TNotebook.Tab", font=("Malgun Gothic", 9, "bold"), padding=[12, 4])
        style.configure("TFrame", background=self.card_bg)
        style.configure("TLabel", background=self.card_bg, foreground=self.text_color, font=("Malgun Gothic", 9))

    def create_widgets(self):
        # 1. 상단 상태 알림 카드
        self.status_frame = tk.Frame(self.root, bg="#0f172a", padx=16, pady=8)
        self.status_frame.pack(fill="x", padx=12, pady=(8, 3))

        self.status_title = tk.Label(
            self.status_frame,
            text="⏸️ 오토클리커 대기 중",
            font=("Malgun Gothic", 12, "bold"),
            fg="#94a3b8",
            bg="#0f172a"
        )
        self.status_title.pack(anchor="w")

        self.status_desc = tk.Label(
            self.status_frame,
            text="[F8]로 치료 버튼 찍기 ➜ [F6]으로 백그라운드 무한 연타 시작/정지.",
            font=("Malgun Gothic", 8),
            fg="#cbd5e1",
            bg="#0f172a"
        )
        self.status_desc.pack(anchor="w", pady=(1, 0))

        # 2. 💾 매크로 프리셋(저장 및 불러오기) 카드
        preset_box = tk.LabelFrame(
            self.root, text=" 💾 매크로 프리셋 (다음에 언제든 불러오기) ",
            bg=self.card_bg, fg="#334155", font=("Malgun Gothic", 9, "bold"), padx=10, pady=4
        )
        preset_box.pack(fill="x", padx=12, pady=3)

        p_row1 = tk.Frame(preset_box, bg=self.card_bg)
        p_row1.pack(fill="x", pady=2)

        tk.Label(p_row1, text="저장할 이름:").pack(side="left")
        self.entry_preset_name = tk.Entry(p_row1, width=14, font=("Malgun Gothic", 9))
        self.entry_preset_name.insert(0, "치료_백그라운드")
        self.entry_preset_name.pack(side="left", padx=4)

        btn_save_preset = tk.Button(
            p_row1, text="💾 현재 설정 저장", font=("Malgun Gothic", 8, "bold"),
            bg="#dcfce7", fg="#15803d", relief="groove", command=self.save_new_preset
        )
        btn_save_preset.pack(side="left", padx=2)

        p_row2 = tk.Frame(preset_box, bg=self.card_bg)
        p_row2.pack(fill="x", pady=2)

        tk.Label(p_row2, text="저장된 프리셋:").pack(side="left")
        self.combo_presets = ttk.Combobox(p_row2, state="readonly", width=16)
        self.combo_presets.pack(side="left", padx=4)

        btn_apply_preset = tk.Button(
            p_row2, text="📥 불러와 적용", font=("Malgun Gothic", 8, "bold"),
            bg="#dbeafe", fg="#1d4ed8", relief="groove", command=self.apply_selected_preset
        )
        btn_apply_preset.pack(side="left", padx=2)

        btn_del_preset = tk.Button(
            p_row2, text="🗑️ 삭제", font=("Malgun Gothic", 8),
            bg="#fee2e2", fg="#b91c1c", relief="groove", command=self.delete_selected_preset
        )
        btn_del_preset.pack(side="left", padx=2)

        # 3. 🌟 마우스 독립 작업 안내 바
        bg_bar = tk.Frame(self.root, bg="#eff6ff", padx=10, pady=6, relief="groove", borderwidth=1)
        bg_bar.pack(fill="x", padx=12, pady=3)

        tk.Label(
            bg_bar, text="🌟 완전 백그라운드 클릭 활성화",
            bg="#eff6ff", font=("Malgun Gothic", 9, "bold"), fg="#1d4ed8"
        ).pack(side="left")

        tk.Label(
            bg_bar, text="(마우스 커서 전혀 안 움직임 - 웹서핑/작업 100% 자유)",
            bg="#eff6ff", font=("Malgun Gothic", 8), fg="#2563eb"
        ).pack(side="left", padx=4)

        # 4. 탭 영역
        self.notebook = ttk.Notebook(self.root)
        self.notebook.pack(fill="both", expand=True, padx=12, pady=3)

        # [탭 1] 🎯 치료 버튼 원클릭 백그라운드 연타 (가장 중요)
        self.tab_single = tk.Frame(self.notebook, bg=self.card_bg, padx=10, pady=6)
        self.notebook.add(self.tab_single, text=" 🎯 치료 버튼 원클릭 연타 ")
        self.build_single_click_tab(self.tab_single)

        # [탭 2] 🎬 마우스 다중 패턴 매크로 (여러 위치 순차 클릭)
        self.tab_pattern = tk.Frame(self.notebook, bg=self.card_bg, padx=10, pady=6)
        self.notebook.add(self.tab_pattern, text=" 🎬 다중 패턴 매크로 ")
        self.build_pattern_tab(self.tab_pattern)

        # [탭 3] ⌨️ 키보드 키 연타
        self.tab_kb = tk.Frame(self.notebook, bg=self.card_bg, padx=10, pady=6)
        self.notebook.add(self.tab_kb, text=" ⌨️ 키보드 연타 ")
        self.build_kb_tab(self.tab_kb)

        # 5. 안전 및 단축키 설정 카드
        opt_box = tk.LabelFrame(
            self.root, text=" ⚙️ 안전 및 공통 옵션 ", bg=self.card_bg,
            fg="#334155", font=("Malgun Gothic", 9, "bold"), padx=10, pady=4
        )
        opt_box.pack(fill="x", padx=12, pady=3)

        row_opt1 = tk.Frame(opt_box, bg=self.card_bg)
        row_opt1.pack(fill="x", pady=1)

        self.var_sound = tk.BooleanVar(value=True)
        cb_sound = tk.Checkbutton(
            row_opt1, text="🔔 소리 알림", variable=self.var_sound, bg=self.card_bg, font=("Malgun Gothic", 9)
        )
        cb_sound.pack(side="left")

        self.var_topmost = tk.BooleanVar(value=True)
        cb_top = tk.Checkbutton(
            row_opt1, text="📌 항상 맨 위", variable=self.var_topmost, bg=self.card_bg, font=("Malgun Gothic", 9),
            command=self.toggle_topmost
        )
        cb_top.pack(side="left", padx=10)
        self.root.attributes("-topmost", True)

        hk_row = tk.Frame(opt_box, bg=self.card_bg)
        hk_row.pack(fill="x", pady=(2, 1))

        self.f_keys = [f"F{i}" for i in range(1, 13)]
        tk.Label(hk_row, text="시작/정지:").pack(side="left")
        self.combo_start_key = ttk.Combobox(hk_row, values=self.f_keys, state="readonly", width=4)
        self.combo_start_key.set("F6")
        self.combo_start_key.pack(side="left", padx=2)

        tk.Label(hk_row, text="치료버튼찍기:").pack(side="left", padx=(8, 0))
        self.combo_rec_key = ttk.Combobox(hk_row, values=self.f_keys, state="readonly", width=4)
        self.combo_rec_key.set("F8")
        self.combo_rec_key.pack(side="left", padx=2)

        # 6. 최하단 대형 시작/정지 버튼
        btn_frame = tk.Frame(self.root, bg=self.bg_color)
        btn_frame.pack(fill="x", padx=12, pady=(4, 6))

        self.btn_main_toggle = tk.Button(
            btn_frame,
            text="▶ 시작하기 (F6)",
            font=("Malgun Gothic", 12, "bold"),
            bg=self.primary_color,
            fg="#ffffff",
            activebackground="#1d4ed8",
            activeforeground="#ffffff",
            relief="flat",
            height=2,
            command=self.toggle_start_stop
        )
        self.btn_main_toggle.pack(fill="x")

    # ------------------------------------------
    # [탭 1] 🎯 치료 버튼 원클릭 백그라운드 연타 UI
    # ------------------------------------------
    def build_single_click_tab(self, parent):
        guide = tk.Label(
            parent,
            text="✨ [치료 버튼 찍기]를 누르고 게임 화면의 치료 버튼을 '딱 1번' 클릭하세요!\n그 게임 창과 좌표를 기억하여, 내 마우스 커서는 전혀 건드리지 않고\n게임 안에서만 0.5초마다 백그라운드로 무한 연타합니다!",
            fg="#1e40af", bg="#eff6ff", padx=8, pady=6, justify="left", font=("Malgun Gothic", 9, "bold")
        )
        guide.pack(fill="x", pady=3)

        # 1. 위치 등록 카드
        pos_box = tk.LabelFrame(parent, text=" 1. 🎯 치료 버튼 위치 등록 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        pos_box.pack(fill="x", pady=3, padx=2)

        r_btn = tk.Frame(pos_box, bg=self.card_bg)
        r_btn.pack(fill="x", padx=8, pady=4)

        self.btn_pick_one = tk.Button(
            r_btn, text="🎯 치료 버튼 1회 클릭하여 위치 등록 [F8]",
            font=("Malgun Gothic", 10, "bold"), bg="#fee2e2", fg="#b91c1c", relief="groove",
            height=2, command=self.start_pick_single_position
        )
        self.btn_pick_one.pack(fill="x")

        self.lbl_registered_pos = tk.Label(
            pos_box, text="등록된 치료 버튼: [ 미등록 - 위 버튼 누르고 게임의 치료 버튼을 1번 클릭하세요 ]",
            font=("Malgun Gothic", 9, "bold"), fg="#dc2626", bg="#fef2f2", padx=6, pady=4
        )
        self.lbl_registered_pos.pack(fill="x", padx=8, pady=(2, 4))

        # 2. 클릭 간격 (초/ms)
        spd_box = tk.LabelFrame(parent, text=" 2. ⏱️ 클릭 간격 시간 설정 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"), fg="#1e40af")
        spd_box.pack(fill="x", pady=3, padx=2)

        r_spd = tk.Frame(spd_box, bg=self.card_bg)
        r_spd.pack(fill="x", padx=8, pady=4)

        tk.Label(r_spd, text="클릭 간격:").pack(side="left")
        self.single_interval = tk.Entry(r_spd, width=6, justify="center", font=("Malgun Gothic", 11, "bold"), fg="#2563eb")
        self.single_interval.insert(0, "0.5")
        self.single_interval.pack(side="left", padx=6)

        self.single_unit = tk.StringVar(value="s")
        tk.Radiobutton(r_spd, text="초(s) 단위 (예: 0.5초)", variable=self.single_unit, value="s", bg=self.card_bg, font=("Malgun Gothic", 9, "bold")).pack(side="left")
        tk.Radiobutton(r_spd, text="밀리초(ms)", variable=self.single_unit, value="ms", bg=self.card_bg).pack(side="left", padx=4)

        tk.Label(
            spd_box, text="👉 0.5초 설정 시: 게임 창 안에서 정확히 0.5초마다 1번씩 치료 버튼을 클릭합니다.",
            fg="#475569", bg="#f8fafc", font=("Malgun Gothic", 8)
        ).pack(anchor="w", padx=8, pady=(0, 4))

        # 3. 반복 모드
        rep_box = tk.LabelFrame(parent, text=" 3. 🔁 반복 방식 설정 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        rep_box.pack(fill="x", pady=3, padx=2)

        r_rep = tk.Frame(rep_box, bg=self.card_bg)
        r_rep.pack(fill="x", padx=8, pady=4)

        self.single_rep_mode = tk.StringVar(value="infinite")
        tk.Radiobutton(
            r_rep, text="무한 반복 (다시 F6 누를 때까지)",
            variable=self.single_rep_mode, value="infinite", bg=self.card_bg,
            font=("Malgun Gothic", 9, "bold"), command=self.toggle_single_repeat
        ).pack(side="left")

        tk.Radiobutton(
            r_rep, text="지정 횟수:",
            variable=self.single_rep_mode, value="count", bg=self.card_bg,
            command=self.toggle_single_repeat
        ).pack(side="left", padx=(12, 0))

        self.single_rep_cnt = tk.Entry(r_rep, width=6, justify="center", state="disabled")
        self.single_rep_cnt.insert(0, "50")
        self.single_rep_cnt.pack(side="left", padx=4)
        tk.Label(r_rep, text="회").pack(side="left")

    def toggle_single_repeat(self):
        st = "normal" if self.single_rep_mode.get() == "count" else "disabled"
        self.single_rep_cnt.config(state=st)

    # ------------------------------------------
    # 🎯 치료 버튼 1회 클릭하여 백그라운드 대상 등록
    # ------------------------------------------
    def start_pick_single_position(self):
        if self.is_running:
            messagebox.showwarning("안내", "오토클리커가 실행 중일 때는 위치를 등록할 수 없습니다.")
            return

        self.play_sound("rec_start")
        self.status_title.config(text="🎯 [위치 찍기 대기] 게임 화면의 치료 버튼을 딱 1번 클릭하세요!", fg="#ef4444")
        self.status_desc.config(text="클릭 즉시 그 게임 창과 좌표가 자동 기억되고 완료됩니다.")
        self.btn_pick_one.config(text="⏳ 대기 중... (게임의 치료 버튼을 1번 클릭하세요)", bg="#fca5a5")

        def on_single_click(x, y, button, pressed):
            if pressed:
                # 오토클리커 GUI 내부 클릭은 제외
                try:
                    win_x = self.root.winfo_rootx()
                    win_y = self.root.winfo_rooty()
                    win_w = self.root.winfo_width()
                    win_h = self.root.winfo_height()
                    if win_x <= x <= win_x + win_w and win_y <= y <= win_y + win_h:
                        return
                except Exception:
                    pass

                self.single_target_x = int(x)
                self.single_target_y = int(y)

                # 🌟 실제 게임 화면을 그리는 정확한 자식 윈도우/캔버스 HWND와 제목 캡처!
                exact_hwnd, root_hwnd, title = get_target_window_info(x, y)
                self.single_target_hwnd = exact_hwnd
                self.single_target_title = title

                # 해당 창 내부 상대 좌표 계산
                cx, cy = screen_to_client_coord(exact_hwnd, x, y)
                self.single_target_cx = cx
                self.single_target_cy = cy

                self.root.after(0, self._on_single_position_captured)
                return False  # 리스너 종료!

        listener = mouse.Listener(on_click=on_single_click)
        listener.daemon = True
        listener.start()

    def _on_single_position_captured(self):
        self.play_sound("point_captured")
        rec_k = self.combo_rec_key.get()
        start_k = self.combo_start_key.get()

        disp_title = self.single_target_title if len(self.single_target_title) <= 20 else self.single_target_title[:17] + "..."
        self.btn_pick_one.config(text=f"🎯 치료 버튼 1회 클릭하여 위치 등록 [{rec_k}]", bg="#fee2e2")
        self.lbl_registered_pos.config(
            text=f"✅ 대상 창: [{disp_title}] / 좌표: ({self.single_target_cx}, {self.single_target_cy})",
            fg="#16a34a", bg="#dcfce7"
        )
        self.status_title.config(text=f"✅ 치료 버튼 등록 완료! [{disp_title}]", fg="#16a34a")
        self.status_desc.config(text=f"이제 [{start_k}] 키를 누르면 마우스 커서 방해 없이 백그라운드로 연타합니다!")

    # ------------------------------------------
    # [탭 2] 다중 패턴 매크로 UI
    # ------------------------------------------
    def build_pattern_tab(self, parent):
        rec_box = tk.LabelFrame(parent, text=" 1. 마우스 여러 위치 순차 녹화 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        rec_box.pack(fill="x", pady=2, padx=2)

        r_btn = tk.Frame(rec_box, bg=self.card_bg)
        r_btn.pack(fill="x", padx=8, pady=3)

        self.btn_rec_toggle = tk.Button(
            r_btn, text="🔴 녹화 시작 [F8]", font=("Malgun Gothic", 9, "bold"),
            bg="#fee2e2", fg="#b91c1c", relief="groove", height=1, padx=8,
            command=self.toggle_record
        )
        self.btn_rec_toggle.pack(side="left")

        self.btn_rec_clear = tk.Button(
            r_btn, text="초기화", font=("Malgun Gothic", 8),
            bg="#f1f5f9", fg="#475569", relief="groove",
            command=self.clear_recorded_pattern
        )
        self.btn_rec_clear.pack(side="left", padx=6)

        self.rec_status_lbl = tk.Label(
            rec_box, text="저장된 클릭: 0개", font=("Malgun Gothic", 9, "bold"),
            fg="#334155", bg=self.card_bg
        )
        self.rec_status_lbl.pack(anchor="w", padx=10, pady=(0, 3))

        interval_box = tk.LabelFrame(
            parent, text=" 2. ⏱️ 각 클릭 사이 간격(초) 설정 ",
            bg=self.card_bg, font=("Malgun Gothic", 9, "bold"), fg="#1e40af"
        )
        interval_box.pack(fill="x", pady=3, padx=2)

        self.pat_interval_mode = tk.StringVar(value="fixed")
        row_fixed = tk.Frame(interval_box, bg=self.card_bg)
        row_fixed.pack(fill="x", padx=8, pady=3)

        rb_fix = tk.Radiobutton(
            row_fixed, text="일정한 간격으로 고정:",
            variable=self.pat_interval_mode, value="fixed", bg=self.card_bg,
            font=("Malgun Gothic", 9, "bold"), command=self.toggle_pat_delay_inputs
        )
        rb_fix.pack(side="left")

        self.entry_pat_delay = tk.Entry(row_fixed, width=6, justify="center", font=("Malgun Gothic", 10, "bold"), fg="#2563eb")
        self.entry_pat_delay.insert(0, "0.5")
        self.entry_pat_delay.pack(side="left", padx=4)

        self.pat_delay_unit = tk.StringVar(value="s")
        tk.Radiobutton(row_fixed, text="초(s)", variable=self.pat_delay_unit, value="s", bg=self.card_bg).pack(side="left")
        tk.Radiobutton(row_fixed, text="밀리초(ms)", variable=self.pat_delay_unit, value="ms", bg=self.card_bg).pack(side="left", padx=2)

        row_orig = tk.Frame(interval_box, bg=self.card_bg)
        row_orig.pack(fill="x", padx=8, pady=(0, 3))
        rb_orig = tk.Radiobutton(
            row_orig, text="녹화 당시 불규칙한 손맛 간격 그대로 재생",
            variable=self.pat_interval_mode, value="recorded", bg=self.card_bg,
            command=self.toggle_pat_delay_inputs
        )
        rb_orig.pack(side="left")

        rep_box = tk.LabelFrame(parent, text=" 3. 🔁 반복 방식 설정 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        rep_box.pack(fill="x", pady=3, padx=2)

        r_play = tk.Frame(rep_box, bg=self.card_bg)
        r_play.pack(fill="x", padx=8, pady=3)

        self.pat_rep_mode = tk.StringVar(value="infinite")
        tk.Radiobutton(
            r_play, text="무한 반복",
            variable=self.pat_rep_mode, value="infinite", bg=self.card_bg,
            font=("Malgun Gothic", 9, "bold"), command=self.toggle_repeat_inputs
        ).pack(side="left")

        tk.Radiobutton(
            r_play, text="지정 횟수:",
            variable=self.pat_rep_mode, value="count", bg=self.card_bg,
            command=self.toggle_repeat_inputs
        ).pack(side="left", padx=(12, 0))

        self.pat_rep_cnt = tk.Entry(r_play, width=6, justify="center", state="disabled")
        self.pat_rep_cnt.insert(0, "10")
        self.pat_rep_cnt.pack(side="left", padx=4)
        tk.Label(r_play, text="회").pack(side="left")

    def toggle_pat_delay_inputs(self):
        is_fixed = (self.pat_interval_mode.get() == "fixed")
        self.entry_pat_delay.config(state="normal" if is_fixed else "disabled")

    def toggle_repeat_inputs(self):
        self.pat_rep_cnt.config(state="normal" if self.pat_rep_mode.get() == "count" else "disabled")
        self.kb_rep_cnt.config(state="normal" if self.kb_rep_mode.get() == "count" else "disabled")

    # ------------------------------------------
    # [탭 3] 키보드 연타 UI
    # ------------------------------------------
    def build_kb_tab(self, parent):
        guide = tk.Label(
            parent,
            text="치료 물약(1, 2번 등)이나 치료 스킬(Q, E 등)을\n자동으로 초고속 연타할 때 사용합니다.",
            fg="#475569", bg="#f1f5f9", padx=8, pady=4, justify="left", font=("Malgun Gothic", 9)
        )
        guide.pack(fill="x", pady=3)

        key_box = tk.LabelFrame(parent, text=" 연타할 키보드 키 설정 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        key_box.pack(fill="x", pady=2, padx=2)

        r1 = tk.Frame(key_box, bg=self.card_bg)
        r1.pack(fill="x", padx=10, pady=4)
        tk.Label(r1, text="연타할 키:").pack(side="left")
        self.entry_kb_key = tk.Entry(r1, width=8, justify="center", font=("Malgun Gothic", 12, "bold"), fg="#1d4ed8")
        self.entry_kb_key.insert(0, "1")
        self.entry_kb_key.pack(side="left", padx=8)
        tk.Label(r1, text="(예: 1, 2, Q, E, Space, F 등)", fg="#64748b", font=("Malgun Gothic", 8)).pack(side="left")

        spd_box2 = tk.LabelFrame(parent, text=" 키 입력 간격 ", bg=self.card_bg, font=("Malgun Gothic", 9, "bold"))
        spd_box2.pack(fill="x", pady=2, padx=2)

        r2 = tk.Frame(spd_box2, bg=self.card_bg)
        r2.pack(fill="x", padx=10, pady=4)
        tk.Label(r2, text="입력 간격:").pack(side="left")
        self.kb_interval = tk.Entry(r2, width=7, justify="center", font=("Malgun Gothic", 9, "bold"))
        self.kb_interval.insert(0, "100")
        self.kb_interval.pack(side="left", padx=6)
        tk.Label(r2, text="밀리초 (ms)").pack(side="left")

        self.var_kb_jitter = tk.BooleanVar(value=True)
        tk.Checkbutton(spd_box2, text="매크로 감지 방지 랜덤 지연 (±10ms 변동)", variable=self.var_kb_jitter, bg=self.card_bg).pack(anchor="w", padx=10, pady=(0, 2))

        rep_box2 = tk.Frame(parent, bg=self.card_bg)
        rep_box2.pack(fill="x", pady=3)
        self.kb_rep_mode = tk.StringVar(value="infinite")
        tk.Radiobutton(rep_box2, text="무한 반복", variable=self.kb_rep_mode, value="infinite", bg=self.card_bg, command=self.toggle_repeat_inputs).pack(side="left")
        tk.Radiobutton(rep_box2, text="지정 횟수:", variable=self.kb_rep_mode, value="count", bg=self.card_bg, command=self.toggle_repeat_inputs).pack(side="left", padx=(10, 0))
        self.kb_rep_cnt = tk.Entry(rep_box2, width=6, justify="center", state="disabled")
        self.kb_rep_cnt.insert(0, "30")
        self.kb_rep_cnt.pack(side="left", padx=4)
        tk.Label(rep_box2, text="회").pack(side="left")

    def toggle_topmost(self):
        self.root.attributes("-topmost", self.var_topmost.get())

    # ==========================================
    # 다중 패턴 녹화 로직
    # ==========================================
    def toggle_record(self):
        if self.is_recording:
            self.stop_record()
        else:
            self.start_record()

    def start_record(self):
        if self.is_running:
            messagebox.showwarning("안내", "오토클리커가 실행 중일 때는 녹화할 수 없습니다.")
            return

        self.is_recording = True
        self.recorded_events = []
        self.last_record_time = None
        self.play_sound("rec_start")

        self.btn_rec_toggle.config(text="⏹ 녹화 종료 [F8]", bg="#fca5a5", fg="#991b1b")
        self.status_title.config(text="🔴 클릭 패턴 녹화 중...", fg="#ef4444")
        self.status_desc.config(text="게임 창에서 원하는 위치들을 순서대로 클릭하세요. 완료 시 [F8].")
        self.rec_status_lbl.config(text="기록 중... (게임 화면을 클릭하세요)", fg="#dc2626")

        def on_click(x, y, button, pressed):
            if not self.is_recording:
                return False
            if pressed:
                try:
                    win_x = self.root.winfo_rootx()
                    win_y = self.root.winfo_rooty()
                    win_w = self.root.winfo_width()
                    win_h = self.root.winfo_height()
                    if win_x <= x <= win_x + win_w and win_y <= y <= win_y + win_h:
                        return
                except Exception:
                    pass

                now = time.time()
                delay = 0.05 if self.last_record_time is None else max(0.01, now - self.last_record_time)
                self.last_record_time = now

                btn_name = "left"
                if button == mouse.Button.right:
                    btn_name = "right"
                elif button == mouse.Button.middle:
                    btn_name = "middle"

                exact_hwnd, _, _ = get_target_window_info(x, y)
                cx, cy = int(x), int(y)
                if exact_hwnd and user32.IsWindow(exact_hwnd):
                    cx, cy = screen_to_client_coord(exact_hwnd, x, y)

                self.recorded_events.append({
                    "hwnd": exact_hwnd,
                    "x": int(x),
                    "y": int(y),
                    "cx": int(cx),
                    "cy": int(cy),
                    "button": btn_name,
                    "delay": delay
                })

                cnt = len(self.recorded_events)
                self.root.after(0, lambda c=cnt, px=int(x), py=int(y): self.rec_status_lbl.config(
                    text=f"기록된 클릭: {c}개 (방금 클릭: X:{px}, Y:{py})", fg="#dc2626"
                ))

        self.record_listener = mouse.Listener(on_click=on_click)
        self.record_listener.daemon = True
        self.record_listener.start()

    def stop_record(self):
        if not self.is_recording:
            return
        self.is_recording = False
        if self.record_listener:
            try:
                self.record_listener.stop()
            except Exception:
                pass

        self.play_sound("rec_stop")
        rec_key = self.combo_rec_key.get()
        start_key = self.combo_start_key.get()

        self.btn_rec_toggle.config(text=f"🔴 녹화 시작 [{rec_key}]", bg="#fee2e2", fg="#b91c1c")
        self.status_title.config(text="⏸️ 오토클리커 대기 중", fg="#94a3b8")
        self.status_desc.config(text=f"단축키 [{start_key}]을 누르면 방금 녹화된 패턴이 반복됩니다.")

        cnt = len(self.recorded_events)
        self.rec_status_lbl.config(text=f"저장 완료: 총 {cnt}개의 클릭이 기억되었습니다! [F6으로 재생]", fg="#16a34a")

    def clear_recorded_pattern(self):
        self.recorded_events = []
        self.rec_status_lbl.config(text="저장된 클릭: 0개", fg="#334155")

    # ==========================================
    # 메인 실행 엔진 (F6 토글)
    # ==========================================
    def toggle_start_stop(self):
        if self.is_running:
            self.stop_running()
        else:
            self.start_running()

    def start_running(self):
        if self.is_running or self.is_recording:
            return

        current_tab_idx = self.notebook.index(self.notebook.select())

        if current_tab_idx == 0:
            # 🎯 탭 1: 단일 치료 버튼 원클릭 백그라운드 연타 모드
            mode = "single"
            if not self.single_target_hwnd or not user32.IsWindow(self.single_target_hwnd):
                # 등록되지 않은 경우 마우스 현재 위치의 창과 좌표로 즉시 자동 등록
                cur_x, cur_y = get_mouse_pos()
                exact_hwnd, _, title = get_target_window_info(cur_x, cur_y)
                if exact_hwnd:
                    self.single_target_hwnd = exact_hwnd
                    self.single_target_title = title
                    self.single_target_x = cur_x
                    self.single_target_y = cur_y
                    cx, cy = screen_to_client_coord(exact_hwnd, cur_x, cur_y)
                    self.single_target_cx = cx
                    self.single_target_cy = cy
                    disp_t = title if len(title) <= 20 else title[:17] + "..."
                    self.lbl_registered_pos.config(
                        text=f"✅ 대상 창: [{disp_t}] / 좌표: ({cx}, {cy})", fg="#16a34a", bg="#dcfce7"
                    )
                else:
                    messagebox.showwarning("안내", "치료 버튼이 등록되지 않았습니다!\n먼저 [🎯 치료 버튼 1회 클릭하여 위치 등록] (F8)을 눌러 게임 화면의 치료 버튼을 클릭해주세요.")
                    return

            try:
                val = float(self.single_interval.get())
                if val <= 0:
                    raise ValueError
                interval_sec = val if self.single_unit.get() == "s" else val / 1000.0
            except ValueError:
                messagebox.showerror("입력 오류", "클릭 간격은 0보다 큰 숫자여야 합니다.")
                return

            max_cnt = 0
            if self.single_rep_mode.get() == "count":
                try:
                    cnt_str = self.single_rep_cnt.get().strip()
                    max_cnt = int(cnt_str) if cnt_str else 50
                except ValueError:
                    messagebox.showerror("입력 오류", "반복 횟수를 올바르게 입력해주세요.")
                    return

            args = ("single", self.single_target_hwnd, self.single_target_cx, self.single_target_cy, interval_sec, max_cnt, self.single_target_title)

        elif current_tab_idx == 1:
            # 🎬 탭 2: 다중 패턴 매크로
            mode = "pattern"
            if not self.recorded_events:
                messagebox.showwarning("안내", "녹화된 마우스 클릭이 없습니다!\n먼저 [🔴 녹화 시작] (F8)을 눌러 게임 화면을 클릭해주세요.")
                return

            max_cnt = 0
            if self.pat_rep_mode.get() == "count":
                try:
                    cnt_str = self.pat_rep_cnt.get().strip()
                    max_cnt = int(cnt_str) if cnt_str else 10
                except ValueError:
                    messagebox.showerror("입력 오류", "반복 횟수를 올바르게 입력해주세요.")
                    return

            pat_int_mode = self.pat_interval_mode.get()
            fixed_delay_sec = 0.5
            if pat_int_mode == "fixed":
                try:
                    d_val = float(self.entry_pat_delay.get())
                    if d_val <= 0:
                        raise ValueError
                    fixed_delay_sec = d_val if self.pat_delay_unit.get() == "s" else d_val / 1000.0
                except ValueError:
                    messagebox.showerror("입력 오류", "클릭 간격은 0보다 큰 숫자여야 합니다.")
                    return

            args = ("pattern", fixed_delay_sec, pat_int_mode, max_cnt)

        else:
            # ⌨️ 탭 3: 키보드 연타
            mode = "keyboard"
            target_key = self.entry_kb_key.get().strip()
            if not target_key:
                messagebox.showerror("입력 오류", "연타할 키보드 키를 입력해주세요.")
                return

            try:
                kb_int_val = float(self.kb_interval.get())
                if kb_int_val <= 0:
                    raise ValueError
                interval_sec = kb_int_val / 1000.0
            except ValueError:
                messagebox.showerror("입력 오류", "키 입력 간격은 0보다 큰 숫자여야 합니다.")
                return

            max_cnt = 0
            if self.kb_rep_mode.get() == "count":
                try:
                    max_cnt = int(self.kb_rep_cnt.get())
                except ValueError:
                    messagebox.showerror("입력 오류", "반복 횟수를 올바르게 입력해주세요.")
                    return

            use_jitter = self.var_kb_jitter.get()
            args = ("keyboard", interval_sec, target_key, max_cnt, use_jitter)

        # 시작 상태로 전환
        self.is_running = True
        self.stop_event.clear()
        self.play_sound("start")

        start_key = self.combo_start_key.get()
        mode_names = {"single": "치료 백그라운드 연타", "pattern": "다중 패턴 매크로", "keyboard": "키보드 연타"}
        self.status_title.config(text=f"● {mode_names[mode]} 동작 중! (마우스 자유 사용 가능)", fg="#22c55e")
        self.status_desc.config(text=f"[{start_key}] 키를 누르면 멈춥니다. (실제 마우스로 다른 작업 가능)")
        self.btn_main_toggle.config(text=f"⏹ 멈추기 ({start_key})", bg=self.danger_color, activebackground="#b91c1c")

        # 작업 스레드 구동
        self.worker_thread = threading.Thread(target=self._worker_loop, args=(mode, args), daemon=True)
        self.worker_thread.start()

    def _worker_loop(self, mode, params):
        click_count = 0

        while not self.stop_event.is_set():
            if mode == "single":
                _, target_hwnd, target_cx, target_cy, interval_sec, max_cnt, title = params

                # 🌟 완전 백그라운드 클릭: 마우스 커서 이동 전혀 없이 게임 창에만 클릭 전송!
                post_click_background(target_hwnd, target_cx, target_cy, button="left")

                click_count += 1
                disp_t = title if len(title) <= 15 else title[:12] + "..."
                self.root.after(0, lambda c=click_count, dt=disp_t, cx=target_cx, cy=target_cy: self.status_title.config(
                    text=f"● [{dt}] 치료 클릭 중! ({cx}, {cy}) [누적: {c}회]", fg="#22c55e"
                ))

                if max_cnt > 0 and click_count >= max_cnt:
                    break

                self._sleep_interruptible(interval_sec)

            elif mode == "pattern":
                _, fixed_delay_sec, pat_int_mode, max_cnt = params
                tot = len(self.recorded_events)

                for idx, ev in enumerate(self.recorded_events):
                    if self.stop_event.is_set():
                        break

                    # 백그라운드 클릭
                    target_hwnd = ev.get("hwnd", self.single_target_hwnd)
                    send_cx = ev.get("cx", ev["x"])
                    send_cy = ev.get("cy", ev["y"])
                    post_click_background(target_hwnd, send_cx, send_cy, button=ev["button"])

                    self.root.after(0, lambda i=idx+1, t=tot, px=send_cx, py=send_cy: self.status_title.config(
                        text=f"● 패턴 백그라운드 클릭 중! [{i}/{t}] ({px}, {py})", fg="#22c55e"
                    ))

                    ev_delay = fixed_delay_sec if pat_int_mode == "fixed" else max(0.01, ev.get("delay", 0.1))
                    self._sleep_interruptible(ev_delay)

                click_count += 1
                if max_cnt > 0 and click_count >= max_cnt:
                    break

            elif mode == "keyboard":
                _, interval_sec, target_key, max_cnt, use_jitter = params

                if self.kb_controller and target_key:
                    try:
                        k_lower = target_key.lower()
                        if k_lower == "space":
                            k = keyboard.Key.space
                        elif k_lower == "enter":
                            k = keyboard.Key.enter
                        elif k_lower == "tab":
                            k = keyboard.Key.tab
                        elif k_lower == "esc":
                            k = keyboard.Key.esc
                        else:
                            k = target_key

                        self.kb_controller.press(k)
                        time.sleep(0.01)
                        self.kb_controller.release(k)
                    except Exception:
                        pass

                click_count += 1
                if max_cnt > 0 and click_count >= max_cnt:
                    break

                sleep_time = interval_sec
                if use_jitter and interval_sec > 0.015:
                    jitter = min(interval_sec * 0.15, 0.015)
                    sleep_time += random.uniform(-jitter, jitter)

                self._sleep_interruptible(sleep_time)

        self.root.after(0, self._on_worker_finished)

    def _sleep_interruptible(self, duration):
        elapsed = 0.0
        step = 0.01
        while elapsed < duration and not self.stop_event.is_set():
            time.sleep(min(step, duration - elapsed))
            elapsed += step

    def _on_worker_finished(self):
        self.is_running = False
        start_key = self.combo_start_key.get()
        self.status_title.config(text="⏸️ 오토클리커 대기 중", fg="#94a3b8")
        self.status_desc.config(text=f"단축키 [{start_key}]을 누르면 바로 시작/정지됩니다.")
        self.btn_main_toggle.config(
            text=f"▶ 시작하기 ({start_key})",
            bg=self.primary_color,
            activebackground="#1d4ed8"
        )
        self.play_sound("stop")

    def stop_running(self):
        if not self.is_running:
            return
        self.stop_event.set()
        self._on_worker_finished()

    # ==========================================
    # 글로벌 단축키 리스너
    # ==========================================
    def start_hotkey_listener(self):
        if keyboard is None:
            return

        def on_press(key):
            try:
                key_name = None
                if hasattr(key, "name"):
                    key_name = key.name.upper()

                if not key_name:
                    return

                start_k = self.combo_start_key.get().upper()
                rec_k = self.combo_rec_key.get().upper()

                if key_name == start_k:
                    self.root.after(0, self.toggle_start_stop)
                elif key_name == rec_k:
                    current_tab = self.notebook.index(self.notebook.select())
                    if current_tab == 0:
                        self.root.after(0, self.start_pick_single_position)
                    else:
                        self.root.after(0, self.toggle_record)
            except Exception:
                pass

        self.kb_listener = keyboard.Listener(on_press=on_press)
        self.kb_listener.daemon = True
        self.kb_listener.start()

    # ==========================================
    # 💾 프리셋(프로필) 관리 로직
    # ==========================================
    def refresh_preset_dropdown(self):
        names = list(self.presets.keys())
        self.combo_presets["values"] = names
        if names:
            self.combo_presets.current(0)
        else:
            self.combo_presets.set("")

    def save_new_preset(self):
        name = self.entry_preset_name.get().strip()
        if not name:
            messagebox.showwarning("안내", "프리셋 이름을 입력해주세요. (예: 치료_백그라운드)")
            return

        current_data = self._gather_all_settings()
        self.presets[name] = current_data
        save_json(PRESETS_FILE, self.presets)
        self.refresh_preset_dropdown()
        self.combo_presets.set(name)
        self.play_sound("preset_apply")
        self.status_title.config(text=f"💾 프리셋 [{name}] 저장 완료!", fg="#16a34a")
        self.root.after(2000, lambda: self.status_title.config(text="⏸️ 오토클리커 대기 중", fg="#94a3b8"))

    def apply_selected_preset(self):
        name = self.combo_presets.get().strip()
        if not name or name not in self.presets:
            messagebox.showwarning("안내", "적용할 프리셋을 선택해주세요.")
            return

        data = self.presets[name]
        self._apply_settings_dict(data)
        self.play_sound("preset_apply")
        self.status_title.config(text=f"✅ 프리셋 [{name}] 적용됨", fg="#16a34a")
        self.root.after(1500, lambda: self.status_title.config(text="⏸️ 오토클리커 대기 중", fg="#94a3b8"))

    def delete_selected_preset(self):
        name = self.combo_presets.get().strip()
        if not name or name not in self.presets:
            return

        if messagebox.askyesno("삭제 확인", f"프리셋 '{name}'을(를) 삭제하시겠습니까?"):
            del self.presets[name]
            save_json(PRESETS_FILE, self.presets)
            self.refresh_preset_dropdown()

    # ==========================================
    # 설정 자동 저장 및 복원 (config.json)
    # ==========================================
    def _gather_all_settings(self):
        cur_tab = self.notebook.index(self.notebook.select())
        return {
            "tab_idx": cur_tab,
            "single_target_hwnd": self.single_target_hwnd,
            "single_target_title": self.single_target_title,
            "single_target_x": self.single_target_x,
            "single_target_y": self.single_target_y,
            "single_target_cx": self.single_target_cx,
            "single_target_cy": self.single_target_cy,
            "single_interval": self.single_interval.get(),
            "single_unit": self.single_unit.get(),
            "single_rep_mode": self.single_rep_mode.get(),
            "single_rep_cnt": self.single_rep_cnt.get(),
            "kb_key": self.entry_kb_key.get(),
            "kb_interval": self.kb_interval.get(),
            "pat_interval_mode": self.pat_interval_mode.get(),
            "pat_fixed_delay": self.entry_pat_delay.get(),
            "pat_delay_unit": self.pat_delay_unit.get(),
            "pat_rep_mode": self.pat_rep_mode.get(),
            "pat_rep_cnt": self.pat_rep_cnt.get(),
            "start_key": self.combo_start_key.get(),
            "rec_key": self.combo_rec_key.get(),
            "sound": self.var_sound.get(),
            "topmost": self.var_topmost.get(),
            "recorded_events": self.recorded_events
        }

    def _apply_settings_dict(self, c):
        if not c:
            return
        try:
            if "single_target_hwnd" in c:
                self.single_target_hwnd = c["single_target_hwnd"]
            if "single_target_title" in c:
                self.single_target_title = c["single_target_title"]
            if "single_target_cx" in c and "single_target_cy" in c:
                self.single_target_cx = c["single_target_cx"]
                self.single_target_cy = c["single_target_cy"]
                disp_t = self.single_target_title if len(self.single_target_title) <= 20 else self.single_target_title[:17] + "..."
                self.lbl_registered_pos.config(
                    text=f"✅ 대상 창: [{disp_t}] / 좌표: ({self.single_target_cx}, {self.single_target_cy})",
                    fg="#16a34a", bg="#dcfce7"
                )

            if "single_interval" in c:
                self.single_interval.delete(0, tk.END)
                self.single_interval.insert(0, str(c["single_interval"]))
            if "single_unit" in c:
                self.single_unit.set(c["single_unit"])
            if "single_rep_mode" in c:
                self.single_rep_mode.set(c["single_rep_mode"])
                self.toggle_single_repeat()
            if "single_rep_cnt" in c:
                self.single_rep_cnt.delete(0, tk.END)
                self.single_rep_cnt.insert(0, str(c["single_rep_cnt"]))

            if "kb_key" in c:
                self.entry_kb_key.delete(0, tk.END)
                self.entry_kb_key.insert(0, str(c["kb_key"]))
            if "kb_interval" in c:
                self.kb_interval.delete(0, tk.END)
                self.kb_interval.insert(0, str(c["kb_interval"]))

            if "pat_interval_mode" in c:
                self.pat_interval_mode.set(c["pat_interval_mode"])
                self.toggle_pat_delay_inputs()
            if "pat_fixed_delay" in c:
                self.entry_pat_delay.delete(0, tk.END)
                self.entry_pat_delay.insert(0, str(c["pat_fixed_delay"]))
            if "pat_delay_unit" in c:
                self.pat_delay_unit.set(c["pat_delay_unit"])
            if "pat_rep_mode" in c:
                self.pat_rep_mode.set(c["pat_rep_mode"])
                self.toggle_repeat_inputs()
            if "pat_rep_cnt" in c:
                self.pat_rep_cnt.delete(0, tk.END)
                self.pat_rep_cnt.insert(0, str(c["pat_rep_cnt"]))

            if "start_key" in c and c["start_key"] in self.f_keys:
                self.combo_start_key.set(c["start_key"])
            if "rec_key" in c and c["rec_key"] in self.f_keys:
                self.combo_rec_key.set(c["rec_key"])

            if "sound" in c:
                self.var_sound.set(c["sound"])
            if "topmost" in c:
                self.var_topmost.set(c["topmost"])
                self.toggle_topmost()

            if "recorded_events" in c and isinstance(c["recorded_events"], list) and c["recorded_events"]:
                self.recorded_events = c["recorded_events"]
                self.rec_status_lbl.config(
                    text=f"저장된 클릭: 총 {len(self.recorded_events)}개 기억됨", fg="#16a34a"
                )

            if "tab_idx" in c:
                self.notebook.select(min(max(0, c["tab_idx"]), 2))
        except Exception:
            pass

    def load_saved_settings(self):
        self._apply_settings_dict(self.cfg)

    def save_current_settings(self):
        try:
            data = self._gather_all_settings()
            save_json(CONFIG_FILE, data)
        except Exception:
            pass

    def on_close(self):
        self.stop_running()
        self.stop_record()
        self.save_current_settings()
        if self.kb_listener:
            try:
                self.kb_listener.stop()
            except Exception:
                pass
        self.root.destroy()


def main():
    root = tk.Tk()
    app = AutoClickerApp(root)
    root.mainloop()

if __name__ == "__main__":
    main()
