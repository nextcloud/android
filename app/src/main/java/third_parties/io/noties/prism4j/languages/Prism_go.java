/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */

package third_parties.io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.Extend;

import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
@Extend("clike")
public class Prism_go {

  @NonNull
  public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

    final Prism4j.Grammar go = GrammarUtils.extend(
      GrammarUtils.require(prism4j, "clike"),
      "go",
      new GrammarUtils.TokenFilter() {
        @Override
        public boolean test(@NonNull Prism4j.Token token) {
          return !"class-name".equals(token.name());
        }
      },
      token("keyword", pattern(compile("\\b(?:break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go(?:to)?|if|import|interface|map|package|range|return|select|struct|switch|type|var)\\b"))),
      token("boolean", pattern(compile("\\b(?:_|iota|nil|true|false)\\b"))),
      token("operator", pattern(compile("[*\\/%^!=]=?|\\+[=+]?|-[=-]?|\\|[=|]?|&(?:=|&|\\^=?)?|>(?:>=?|=)?|<(?:<=?|=|-)?|:=|\\.\\.\\."))),
      token("number", pattern(compile("(?:\\b0x[a-f\\d]+|(?:\\b\\d+\\.?\\d*|\\B\\.\\d+)(?:e[-+]?\\d+)?)i?", CASE_INSENSITIVE))),
      token("string", pattern(
        compile("([\"'`])(\\\\[\\s\\S]|(?!\\1)[^\\\\])*\\1"),
        false,
        true
      ))
    );

    // clike doesn't have builtin
    GrammarUtils.insertBeforeToken(go, "boolean",
      token("builtin", pattern(compile("\\b(?:bool|byte|complex(?:64|128)|error|float(?:32|64)|rune|string|u?int(?:8|16|32|64)?|uintptr|append|cap|close|complex|copy|delete|imag|len|make|new|panic|print(?:ln)?|real|recover)\\b")))
    );

    return go;
  }
}
