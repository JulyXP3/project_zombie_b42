#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""EtherHack 翻译文件校验 (P5)

用途: 在改动 UI / 翻译后跑一遍, 防止再次出现 "某语言缺键 / 多键 / 重复键 /
Lua 引用了不存在的键 / 翻译里有死键" 这类回归。

检查项:
  1. 编码必须是合法 UTF-8, 且不含 U+FFFD (乱码残留)
  2. 同一文件内不得有重复键
  3. 三种语言的键集合必须完全一致
  4. Lua/Java 中 tr("K") / getTranslate("K") 引用的键必须都已定义
  5. 报告翻译文件里定义了但源码 (Lua+Java) 从未引用的死键 (警告, 不算失败)
  6. 同一键的 {name} 占位符集合在三种语言间必须一致
  7. 文件风格: 统一 4 空格缩进 + 统一保留尾逗号 (方案 D 的格式约定)

用法:
    python tools/check_translations.py            # 校验, 失败返回码 1
    python tools/check_translations.py --verbose  # 额外列出死键明细
"""
from __future__ import print_function

import io
import os
import re
import sys

# Windows 控制台常为 GBK, 直接 print 中文/俄文会乱码 -> 强制 UTF-8 输出
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass
elif hasattr(sys.stdout, 'buffer'):
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8',
                                  errors='replace')

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, 'etherhack-src', 'src', 'main', 'resources', 'EtherHack')
TRANS_DIR = os.path.join(RES, 'translations')
LUA_DIR = os.path.join(RES, 'lua')
JAVA_DIR = os.path.join(ROOT, 'etherhack-src', 'src', 'main', 'java')

LANGS = ('CN', 'EN', 'RU')

# 游戏原生 (vanilla PZ) 翻译键: 我们故意复用, 不该由本 mod 定义。
# 放进白名单以免被误报成 "引用了未定义的键"。
VANILLA_KEYS = set([
    'UI_characreation_forename',
    'UI_characreation_surname',
])

# 形如:  UI_Xxx_Yyy = "....",
KEY_RE = re.compile(r'^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*"((?:[^"\\]|\\.)*)"\s*,?\s*$')
# Lua 侧显式调用: tr("K") / tr('K') / getTranslate("K")
# Java 侧: etherTranslator.getTranslate("K") (同一正则即可覆盖)
CALL_RE = re.compile(r'(?:\btr|\bgetTranslate)\s*\(\s*["\']([A-Za-z_][A-Za-z0-9_]*)["\']')
# 任意 "UI_xxx" 字面量: 迁移到 EtherFormPanel 后, 大量键是以数据表字段形式
# 存放的 (如 { key = "UI_CharacterPanel_GodMode" }), 再由 tr(row.key) 取用,
# 因此不能只认 tr(...) 直接调用, 否则会把它们全误报成死键。
LIT_RE = re.compile(r'["\'](UI_[A-Za-z0-9_]*)["\']')
# 前缀拼接: "UI_Skill_" .. name  -> 该前缀下的键都算被引用
PREFIX_RE = re.compile(r'["\'](UI_[A-Za-z0-9_]*)["\']\s*\.\.')
# 翻译串里的 {name} 占位符 (由 EtherI18n.tr 的第二参替换)
PLACEHOLDER_RE = re.compile(r'\{([A-Za-z_][A-Za-z0-9_]*)\}')

errors = []
warnings = []


def fail(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def load_lang(lang):
    """返回 (有序键列表, {键: 值}); 顺带做编码与重复键检查。"""
    path = os.path.join(TRANS_DIR, '%s.txt' % lang)
    if not os.path.exists(path):
        fail('%s.txt 不存在' % lang)
        return [], {}

    raw = open(path, 'rb').read()
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError as e:
        fail('%s.txt 不是合法 UTF-8: %s' % (lang, e))
        return [], {}

    if u'\ufffd' in text:
        n = text.count(u'\ufffd')
        fail('%s.txt 含 %d 个 U+FFFD 替换字符 (乱码残留)' % (lang, n))

    keys, values, seen = [], {}, {}
    for i, line in enumerate(text.splitlines(), 1):
        s = line.strip()
        if not s or s in ('{', '}') or s.startswith('--'):
            continue
        m = KEY_RE.match(line)
        if not m:
            fail('%s.txt:%d 无法解析: %s' % (lang, i, s[:70]))
            continue
        k, v = m.group(1), m.group(2)

        # 7) 风格约定: 4 空格缩进 + 尾逗号 (禁止 Tab, 禁止漏逗号)
        if '\t' in line:
            fail('%s.txt:%d 使用了 Tab 缩进 (应为 4 空格): %s' % (lang, i, k))
        elif not line.startswith('    ') or line[4:5] == ' ':
            fail('%s.txt:%d 缩进不是 4 个空格: %s' % (lang, i, k))
        if not line.rstrip().endswith(','):
            fail('%s.txt:%d 缺少尾逗号: %s' % (lang, i, k))

        if k in seen:
            fail('%s.txt:%d 重复键 %s (首次出现在第 %d 行)' % (lang, i, k, seen[k]))
        else:
            seen[k] = i
            keys.append(k)
            values[k] = v
    return keys, values


BLOCK_COMMENT_RE = re.compile(r'--\[(=*)\[.*?\]\1\]', re.S)


def strip_lua_comments(src):
    """去掉 Lua 注释, 避免文档示例里的 "UI_Key" 之类被当成真实引用。

    先去块注释 --[[ ]] / --[==[ ]==], 再逐行去行注释 --, 但要跳过字符串里的 --。
    """
    src = BLOCK_COMMENT_RE.sub(' ', src)
    out = []
    for line in src.splitlines():
        res, i, n = [], 0, len(line)
        quote = None
        while i < n:
            c = line[i]
            if quote:
                if c == '\\':
                    res.append(line[i:i + 2])
                    i += 2
                    continue
                if c == quote:
                    quote = None
                res.append(c)
            elif c in '"\'':
                quote = c
                res.append(c)
            elif c == '-' and i + 1 < n and line[i + 1] == '-':
                break          # 行注释, 丢弃余下部分
            else:
                res.append(c)
            i += 1
        out.append(''.join(res))
    return '\n'.join(out)


JAVA_BLOCK_RE = re.compile(r'/\*.*?\*/', re.S)


def strip_java_comments(src):
    """去掉 Java 注释 (/* */ 与 //), 字符串内的 // 不受影响。"""
    src = JAVA_BLOCK_RE.sub(' ', src)
    out = []
    for line in src.splitlines():
        res, i, n = [], 0, len(line)
        quote = None
        while i < n:
            c = line[i]
            if quote:
                if c == '\\':
                    res.append(line[i:i + 2])
                    i += 2
                    continue
                if c == quote:
                    quote = None
                res.append(c)
            elif c in '"\'':
                quote = c
                res.append(c)
            elif c == '/' and i + 1 < n and line[i + 1] == '/':
                break
            else:
                res.append(c)
            i += 1
        out.append(''.join(res))
    return '\n'.join(out)


def scan_sources():
    """扫描 Lua 与 Java 源码。

    返回 (calls, lits, prefixes):
      calls    {键: [位置...]}  tr(..)/getTranslate(..) 的直接调用
      lits     {键: [位置...]}  任意 "UI_xxx" 字面量 (含数据表字段)
      prefixes {前缀: [位置...]} 形如 "UI_Xxx_" .. var 的拼接前缀
    Java 侧 ESP/绘制文本也会用 getTranslate("UI_VisualsDraws_*"),
    所以必须一起扫, 否则这些键会被误判为死键。
    """
    calls, lits, prefixes = {}, {}, {}
    targets = ((LUA_DIR, '.lua', strip_lua_comments),
               (JAVA_DIR, '.java', strip_java_comments))
    for base, ext, stripper in targets:
        if not os.path.isdir(base):
            continue
        for d, _dirs, files in os.walk(base):
            for f in files:
                if not f.endswith(ext):
                    continue
                p = os.path.join(d, f)
                src = open(p, 'rb').read().decode('utf-8', 'replace')
                src = stripper(src)
                for i, line in enumerate(src.splitlines(), 1):
                    where = '%s:%d' % (f, i)
                    for k in CALL_RE.findall(line):
                        calls.setdefault(k, []).append(where)
                    for k in LIT_RE.findall(line):
                        lits.setdefault(k, []).append(where)
                    for k in PREFIX_RE.findall(line):
                        prefixes.setdefault(k, []).append(where)
    return calls, lits, prefixes


def main():
    verbose = '--verbose' in sys.argv

    table = {}
    for lang in LANGS:
        keys, values = load_lang(lang)
        table[lang] = (keys, values)
        print('%s.txt  keys=%d' % (lang, len(keys)))

    # 3) 键集合一致性 (以 CN 为基准做对称差)
    sets = dict((l, set(table[l][0])) for l in LANGS)
    union = set()
    for s in sets.values():
        union |= s
    for lang in LANGS:
        missing = sorted(union - sets[lang])
        if missing:
            fail('%s.txt 缺少 %d 个键: %s' % (lang, len(missing), ', '.join(missing[:12])))

    # 4)/5) 与 Lua + Java 引用交叉比对
    calls, lits, prefixes = scan_sources()

    # 已定义键中, 只要出现过同名字面量就算被引用
    used = set(k for k in lits if k in union)
    # 前缀拼接: 该前缀下的所有已定义键都算被引用
    for pref in prefixes:
        for k in union:
            if k.startswith(pref):
                used.add(k)

    # 4) 显式 tr("UI_...") / 字面量中引用了但翻译文件里没有的键 -> 一定是 bug
    referenced = set(k for k in lits if k.startswith('UI_'))
    referenced |= set(k for k in calls if k.startswith('UI_'))
    undefined = sorted(referenced - union - VANILLA_KEYS)
    for k in undefined:
        pos = (lits.get(k) or calls.get(k) or ['?'])[:4]
        fail('Lua 引用了未定义的翻译键 %s  <- %s' % (k, ', '.join(pos)))

    # 5) 死键 (警告): 排除被前缀拼接覆盖的
    dead = sorted(union - used)
    if dead:
        warn('翻译文件中有 %d 个键未被 Lua/Java 引用 (可能是死键)' % len(dead))
        if verbose:
            for k in dead:
                warn('    dead: %s' % k)

    # 6) 占位符一致性: 同一键在各语言里的 {name} 集合必须相同, 否则某语言
    #    会丢掉数值/原因等动态信息 (tr() 替换不到就什么都不显示)。
    n_ph = 0
    for key in sorted(union):
        per_lang = {}
        for lang in LANGS:
            val = table[lang][1].get(key)
            if val is None:
                continue
            per_lang[lang] = set(PLACEHOLDER_RE.findall(val))
        distinct = set(frozenset(v) for v in per_lang.values())
        if len(distinct) > 1:
            detail = '; '.join(
                '%s={%s}' % (l, ','.join(sorted(per_lang[l])) or '-')
                for l in LANGS if l in per_lang)
            fail('键 %s 的占位符在各语言间不一致: %s' % (key, detail))
        elif distinct and list(distinct)[0]:
            n_ph += 1

    print('')
    print('带占位符的键: %d 个 (各语言占位符集合一致)' % n_ph)
    print('源码引用键 (UI_*): %d 个 (其中 tr()/getTranslate() 直接调用 %d 个)'
          % (len(referenced), len([k for k in calls if k.startswith('UI_')])))
    if prefixes:
        print('前缀拼接: %s' % ', '.join(sorted(prefixes)))
    for w in warnings:
        print('WARN  ' + w)
    for e in errors:
        print('ERROR ' + e)

    print('')
    if errors:
        print('RESULT: FAIL (%d 个错误, %d 个警告)' % (len(errors), len(warnings)))
        return 1
    print('RESULT: OK (%d 键 x %d 语言, %d 个警告)'
          % (len(union), len(LANGS), len(warnings)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
