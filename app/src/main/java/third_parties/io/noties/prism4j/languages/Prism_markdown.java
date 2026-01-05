/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */

package third_parties.io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.Extend;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
@Extend("markup")
public class Prism_markdown {

  @NonNull
  public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

    final Prism4j.Grammar markdown = GrammarUtils.extend(
      GrammarUtils.require(prism4j, "markup"),
      "markdown"
    );

    final Prism4j.Token bold = token("bold", pattern(
      compile("(^|[^\\\\])(\\*\\*|__)(?:(?:\\r?\\n|\\r)(?!\\r?\\n|\\r)|.)+?\\2"),
      true,
      false,
      null,
      grammar("inside", token("punctuation", pattern(compile("^\\*\\*|^__|\\*\\*$|__$"))))
    ));

    final Prism4j.Token italic = token("italic", pattern(
      compile("(^|[^\\\\])([*_])(?:(?:\\r?\\n|\\r)(?!\\r?\\n|\\r)|.)+?\\2"),
      true,
      false,
      null,
      grammar("inside", token("punctuation", pattern(compile("^[*_]|[*_]$"))))
    ));

    final Prism4j.Token url = token("url", pattern(
      compile("!?\\[[^\\]]+\\](?:\\([^\\s)]+(?:[\\t ]+\"(?:\\\\.|[^\"\\\\])*\")?\\)| ?\\[[^\\]\\n]*\\])"),
      false,
      false,
      null,
      grammar("inside",
        token("variable", pattern(compile("(!?\\[)[^\\]]+(?=\\]$)"), true)),
        token("string", pattern(compile("\"(?:\\\\.|[^\"\\\\])*\"(?=\\)$)")))
      )
    ));

    GrammarUtils.insertBeforeToken(markdown, "prolog",
      token("blockquote", pattern(compile("^>(?:[\\t ]*>)*", MULTILINE))),
      token("code",
        pattern(compile("^(?: {4}|\\t).+", MULTILINE), false, false, "keyword"),
        pattern(compile("``.+?``|`[^`\\n]+`"), false, false, "keyword")
      ),
      token(
        "title",
        pattern(
          compile("\\w+.*(?:\\r?\\n|\\r)(?:==+|--+)"),
          false,
          false,
          "important",
          grammar("inside", token("punctuation", pattern(compile("==+$|--+$"))))
        ),
        pattern(
          compile("(^\\s*)#+.+", MULTILINE),
          true,
          false,
          "important",
          grammar("inside", token("punctuation", pattern(compile("^#+|#+$"))))
        )
      ),
      token("hr", pattern(
        compile("(^\\s*)([*-])(?:[\\t ]*\\2){2,}(?=\\s*$)", MULTILINE),
        true,
        false,
        "punctuation"
      )),
      token("list", pattern(
        compile("(^\\s*)(?:[*+-]|\\d+\\.)(?=[\\t ].)", MULTILINE),
        true,
        false,
        "punctuation"
      )),
      token("url-reference", pattern(
        compile("!?\\[[^\\]]+\\]:[\\t ]+(?:\\S+|<(?:\\\\.|[^>\\\\])+>)(?:[\\t ]+(?:\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\((?:\\\\.|[^)\\\\])*\\)))?"),
        false,
        false,
        "url",
        grammar("inside",
          token("variable", pattern(compile("^(!?\\[)[^\\]]+"), true)),
          token("string", pattern(compile("(?:\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\((?:\\\\.|[^)\\\\])*\\))$"))),
          token("punctuation", pattern(compile("^[\\[\\]!:]|[<>]")))
        )
      )),
      bold,
      italic,
      url
    );

    add(GrammarUtils.findFirstInsideGrammar(bold), url, italic);
    add(GrammarUtils.findFirstInsideGrammar(italic), url, bold);

    return markdown;
  }

  private static void add(@Nullable Prism4j.Grammar grammar, @NonNull Prism4j.Token first, @NonNull Prism4j.Token second) {
    if (grammar != null) {
      grammar.tokens().add(first);
      grammar.tokens().add(second);
    }
  }
}
