#!/usr/bin/env python3
"""중국어 간체 글자에서 번체(대만 말투) 글자를 다시 만든다.

간체 쪽(StrZh·AppZh·MoreZh, res/values-zh, assets/manual-zh.html)을 고친 뒤 저장소 맨 위에서 실행한다.
    pip install opencc-python-reimplemented
    python3 tools/zh_tw.py
번체 쪽(StrZhTw·AppZhTw·MoreZhTw, res/values-b+zh+Hant, assets/manual-zh-hant.html)은 손으로 고치지 않는다.
대만에서 쓰는 말로 따로 맞춘 낱말은 아래 FIX에 적는다.
"""
import re
import opencc

BASE = 'android-app/app/src/main/'
SRC = BASE + 'java/com/sejun/autoclicker/'
_cc = opencc.OpenCC('s2twp')
# (변환기가 내놓는 말, 대신 쓸 말). 위에서부터 차례로 바꾼다.
FIX = [('程式碼', '代碼'), ('點選', '點擊'), ('釋出', '發佈'), ('控制檯', '主控台'), ('郵箱', '電子郵件'),
       ('許可權', '權限'), ('繫結', '綁定'), ('臺裝置', '台裝置'), ('臺手機', '台手機'),
       ('應用', '應用程式'), ('已應用程式', '已套用'), ('>應用程式<', '>套用<'), ('“', '「'), ('”', '」')]


def tw(text):
    if text in ('应用', '"应用"'):      # "적용" 버튼. 앱(應用程式)과 같은 글자라 따로 본다
        return text.replace('应用', '套用')
    out = _cc.convert(text)
    for a, b in FIX:
        out = out.replace(a, b)
    return out


def literals(s):
    """코틀린 소스에서 바깥쪽 문자열 리터럴의 (시작, 끝) 목록. 주석은 건너뛴다."""
    out, i, n = [], 0, len(s)

    def skip_string(i):
        if s.startswith('"""', i):
            return s.index('"""', i + 3) + 3
        j = i + 1
        while j < n:
            c = s[j]
            if c == '\\':
                j += 2
            elif c == '"':
                return j + 1
            elif c == '$' and s[j + 1:j + 2] == '{':
                j = skip_block(j + 2)
            else:
                j += 1
        raise ValueError('unterminated string')

    def skip_block(j):
        depth = 1
        while j < n:
            c = s[j]
            if c == '"':
                j = skip_string(j)
                continue
            if c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return j + 1
            j += 1
        raise ValueError('unterminated block')

    while i < n:
        if s.startswith('//', i):
            j = s.find('\n', i)
            i = n if j < 0 else j
        elif s.startswith('/*', i):
            i = s.index('*/', i) + 2
        elif s[i] == '"':
            j = skip_string(i)
            out.append((i, j))
            i = j
        else:
            i += 1
    return out


def kotlin(path, name):
    """파일 안의 `object <name>`을 번체로 바꾼 `object <name>Tw`를 파일 끝에 다시 쓴다."""
    s = open(SRC + path, encoding='utf-8').read()
    start = s.index('internal object %s ' % name)
    end = s.index('\n}\n', start) + 3
    block = s[start:end]
    res, last = [], 0
    for a, b in literals(block):
        res.append(block[last:a])
        res.append(tw(block[a:b]))
        last = b
    res.append(block[last:])
    made = ''.join(res).replace('object %s ' % name, 'object %sTw ' % name, 1)
    old = s.find('internal object %sTw ' % name)
    if old >= 0:
        s = s[:old].rstrip('\n') + '\n'
    else:
        s = s.rstrip('\n') + '\n'
    open(SRC + path, 'w', encoding='utf-8').write(s + '\n' + made)


def main():
    kotlin('Strings.kt', 'StrZh')
    kotlin('StringsApp.kt', 'AppZh')
    kotlin('StringsMore.kt', 'MoreZh')

    x = open(BASE + 'res/values-zh/strings.xml', encoding='utf-8').read()
    head, body = x.split('<resources>')
    open(BASE + 'res/values-b+zh+Hant/strings.xml', 'w', encoding='utf-8').write(
        head.replace('간체', '번체').replace('StrZh', 'StrZhTw') + '<resources>' + tw(body))

    m = open(BASE + 'assets/manual-zh.html', encoding='utf-8').read()
    open(BASE + 'assets/manual-zh-hant.html', 'w', encoding='utf-8').write(
        tw(m).replace('lang="zh-Hans"', 'lang="zh-Hant"'))


if __name__ == '__main__':
    main()
