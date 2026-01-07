/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */

package third_parties.io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.Prism4j;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
public class Prism_python {

  @NonNull
  public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
    return grammar("python",
      token("comment", pattern(
        compile("(^|[^\\\\])#.*"),
        true
      )),
      token("triple-quoted-string", pattern(
        compile("(\"\"\"|''')[\\s\\S]+?\\1"),
        false,
        true,
        "string"
      )),
      token("string", pattern(
        compile("(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1"),
        false,
        true
      )),
      token("function", pattern(
        compile("((?:^|\\s)def[ \\t]+)[a-zA-Z_]\\w*(?=\\s*\\()"),
        true
      )),
      token("class-name", pattern(
        compile("(\\bclass\\s+)\\w+", CASE_INSENSITIVE),
        true
      )),
      token("keyword", pattern(compile("\\b(?:as|assert|async|await|break|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|nonlocal|pass|print|raise|return|try|while|with|yield)\\b"))),
      token("builtin", pattern(compile("\\b(?:__import__|abs|all|any|apply|ascii|basestring|bin|bool|buffer|bytearray|bytes|callable|chr|classmethod|cmp|coerce|compile|complex|delattr|dict|dir|divmod|enumerate|eval|execfile|file|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|intern|isinstance|issubclass|iter|len|list|locals|long|map|max|memoryview|min|next|object|oct|open|ord|pow|property|range|raw_input|reduce|reload|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|unichr|unicode|vars|xrange|zip)\\b"))),
      token("boolean", pattern(compile("\\b(?:True|False|None)\\b"))),
      token("number", pattern(
        compile("(?:\\b(?=\\d)|\\B(?=\\.))(?:0[bo])?(?:(?:\\d|0x[\\da-f])[\\da-f]*\\.?\\d*|\\.\\d+)(?:e[+-]?\\d+)?j?\\b", CASE_INSENSITIVE)
      )),
      token("operator", pattern(compile("[-+%=]=?|!=|\\*\\*?=?|\\/\\/?=?|<[<=>]?|>[=>]?|[&|^~]|\\b(?:or|and|not)\\b"))),
      token("punctuation", pattern(compile("[{}\\[\\];(),.:]")))
    );
  }
}
