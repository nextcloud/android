/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */

package third_parties.io.noties.prism4j.languages;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.Extend;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.compile;


@SuppressWarnings("unused")
@Extend("clike")
public class Prism_kotlin {

  @NotNull
  public static Prism4j.Grammar create(@NotNull Prism4j prism4j) {

    final Prism4j.Grammar kotlin = GrammarUtils.extend(
      GrammarUtils.require(prism4j, "clike"),
      "kotlin",
      new GrammarUtils.TokenFilter() {
        @Override
        public boolean test(@NotNull Prism4j.Token token) {
          return !"class-name".equals(token.name());
        }
      },
      token(
        "keyword",
        pattern(compile("(^|[^.])\\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|to|try|typealias|val|var|vararg|when|where|while)\\b"), true)
      ),
      token(
        "function",
        pattern(compile("\\w+(?=\\s*\\()")),
        pattern(compile("(\\.)\\w+(?=\\s*\\{)"), true)
      ),
      token(
        "number",
        pattern(compile("\\b(?:0[xX][\\da-fA-F]+(?:_[\\da-fA-F]+)*|0[bB][01]+(?:_[01]+)*|\\d+(?:_\\d+)*(?:\\.\\d+(?:_\\d+)*)?(?:[eE][+-]?\\d+(?:_\\d+)*)?[fFL]?)\\b"))
      ),
      token(
        "operator",
        pattern(compile("\\+[+=]?|-[-=>]?|==?=?|!(?:!|==?)?|[\\/*%<>]=?|[?:]:?|\\.\\.|&&|\\|\\||\\b(?:and|inv|or|shl|shr|ushr|xor)\\b"))
      )
    );

    GrammarUtils.insertBeforeToken(kotlin, "string",
      token("raw-string", pattern(compile("(\"\"\"|''')[\\s\\S]*?\\1"), false, false, "string"))
    );

    GrammarUtils.insertBeforeToken(kotlin, "keyword",
      token("annotation", pattern(compile("\\B@(?:\\w+:)?(?:[A-Z]\\w*|\\[[^\\]]+\\])"), false, false, "builtin"))
    );

    GrammarUtils.insertBeforeToken(kotlin, "function",
      token("label", pattern(compile("\\w+@|@\\w+"), false, false, "symbol"))
    );

    // this grammar has 1 token: interpolation, which has 2 patterns
    final Prism4j.Grammar interpolationInside;
    {

      // okay, I was cloning the tokens of kotlin grammar (so there is no recursive chain of calls),
      // but it looks like it wants to have recursive calls
      // I did this because interpolation test was failing due to the fact that `string`
      // `raw-string` tokens didn't have `inside`, so there were not tokenized
      // I still find that it has potential to fall with stackoverflow (in some cases)
      final List<Prism4j.Token> tokens = new ArrayList<>(kotlin.tokens().size() + 1);
      tokens.add(token("delimiter", pattern(compile("^\\$\\{|\\}$"), false, false, "variable")));
      tokens.addAll(kotlin.tokens());

      interpolationInside = grammar(
        "inside",
        token("interpolation",
          pattern(compile("\\$\\{[^}]+\\}"), false, false, null, grammar("inside", tokens)),
          pattern(compile("\\$\\w+"), false, false, "variable")
        )
      );
    }

    final Prism4j.Token string = GrammarUtils.findToken(kotlin, "string");
    final Prism4j.Token rawString = GrammarUtils.findToken(kotlin, "raw-string");

    if (string != null
      && rawString != null) {

      final Prism4j.Pattern stringPattern = string.patterns().get(0);
      final Prism4j.Pattern rawStringPattern = rawString.patterns().get(0);

      string.patterns().add(
        pattern(stringPattern.regex(), stringPattern.lookbehind(), stringPattern.greedy(), stringPattern.alias(), interpolationInside)
      );

      rawString.patterns().add(
        pattern(rawStringPattern.regex(), rawStringPattern.lookbehind(), rawStringPattern.greedy(), rawStringPattern.alias(), interpolationInside)
      );

      string.patterns().remove(0);
      rawString.patterns().remove(0);

    } else {
      throw new RuntimeException("Unexpected state, cannot find `string` and/or `raw-string` tokens " +
        "inside kotlin grammar");
    }

    return kotlin;
  }
}
