/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */

package third_parties.io.noties.prism4j.languages;


import org.jetbrains.annotations.NotNull;

import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.Aliases;
import io.noties.prism4j.annotations.Extend;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
@Aliases("dotnet")
@Extend("clike")
public class Prism_csharp {

  @NotNull
  public static Prism4j.Grammar create(@NotNull Prism4j prism4j) {

    final Prism4j.Grammar classNameInsidePunctuation = grammar("inside",
      token("punctuation", pattern(compile("\\.")))
    );

    final Prism4j.Grammar csharp = GrammarUtils.extend(
      GrammarUtils.require(prism4j, "clike"),
      "csharp",
      token("keyword", pattern(compile("\\b(?:abstract|add|alias|as|ascending|async|await|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|descending|do|double|dynamic|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|from|get|global|goto|group|if|implicit|in|int|interface|internal|into|is|join|let|lock|long|namespace|new|null|object|operator|orderby|out|override|params|partial|private|protected|public|readonly|ref|remove|return|sbyte|sealed|select|set|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|value|var|virtual|void|volatile|where|while|yield)\\b"))),
      token("string",
        pattern(compile("@(\"|')(?:\\1\\1|\\\\[\\s\\S]|(?!\\1)[^\\\\])*\\1"), false, true),
        pattern(compile("(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*?\\1"), false, true)
      ),
      token("class-name",
        pattern(
          compile("\\b[A-Z]\\w*(?:\\.\\w+)*\\b(?=\\s+\\w+)"),
          false,
          false,
          null,
          classNameInsidePunctuation
        ),
        pattern(
          compile("(\\[)[A-Z]\\w*(?:\\.\\w+)*\\b"),
          true,
          false,
          null,
          classNameInsidePunctuation
        ),
        pattern(
          compile("(\\b(?:class|interface)\\s+[A-Z]\\w*(?:\\.\\w+)*\\s*:\\s*)[A-Z]\\w*(?:\\.\\w+)*\\b"),
          true,
          false,
          null,
          classNameInsidePunctuation
        ),
        pattern(
          compile("((?:\\b(?:class|interface|new)\\s+)|(?:catch\\s+\\())[A-Z]\\w*(?:\\.\\w+)*\\b"),
          true,
          false,
          null,
          classNameInsidePunctuation
        )
      ),
      token("number", pattern(compile("\\b0x[\\da-f]+\\b|(?:\\b\\d+\\.?\\d*|\\B\\.\\d+)f?", CASE_INSENSITIVE)))
    );

    GrammarUtils.insertBeforeToken(csharp, "class-name",
      token("generic-method", pattern(
        compile("\\w+\\s*<[^>\\r\\n]+?>\\s*(?=\\()"),
        false,
        false,
        null,
        grammar("inside",
          token("function", pattern(compile("^\\w+"))),
          token("class-name", pattern(compile("\\b[A-Z]\\w*(?:\\.\\w+)*\\b"), false, false, null, classNameInsidePunctuation)),
          GrammarUtils.findToken(csharp, "keyword"),
          token("punctuation", pattern(compile("[<>(),.:]")))
        )
      )),
      token("preprocessor", pattern(
        compile("(^\\s*)#.*", MULTILINE),
        true,
        false,
        "property",
        grammar("inside",
          token("directive", pattern(
            compile("(\\s*#)\\b(?:define|elif|else|endif|endregion|error|if|line|pragma|region|undef|warning)\\b"),
            true,
            false,
            "keyword"
          ))
        )
      ))
    );

    return csharp;
  }
}
