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
import io.noties.prism4j.annotations.Aliases;
import io.noties.prism4j.annotations.Extend;
import io.noties.prism4j.annotations.Modify;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

@SuppressWarnings("unused")
@Aliases("js")
@Modify("markup")
@Extend("clike")
public class Prism_javascript {

  @NotNull
  public static Prism4j.Grammar create(@NotNull Prism4j prism4j) {

    final Prism4j.Grammar js = GrammarUtils.extend(GrammarUtils.require(prism4j, "clike"), "javascript",
      token("keyword", pattern(compile("\\b(?:as|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|var|void|while|with|yield)\\b"))),
      token("number", pattern(compile("\\b(?:0[xX][\\dA-Fa-f]+|0[bB][01]+|0[oO][0-7]+|NaN|Infinity)\\b|(?:\\b\\d+\\.?\\d*|\\B\\.\\d+)(?:[Ee][+-]?\\d+)?"))),
      token("function", pattern(compile("[_$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*(?=\\s*\\()", CASE_INSENSITIVE))),
      token("operator", pattern(compile("-[-=]?|\\+[+=]?|!=?=?|<<?=?|>>?>?=?|=(?:==?|>)?|&[&=]?|\\|[|=]?|\\*\\*?=?|\\/=?|~|\\^=?|%=?|\\?|\\.{3}")))
    );

    GrammarUtils.insertBeforeToken(js, "keyword",
      token("regex", pattern(
        compile("((?:^|[^$\\w\\xA0-\\uFFFF.\"'\\])\\s])\\s*)\\/(\\[[^\\]\\r\\n]+]|\\\\.|[^/\\\\\\[\\r\\n])+\\/[gimyu]{0,5}(?=\\s*($|[\\r\\n,.;})\\]]))"),
        true,
        true
      )),
      token(
        "function-variable",
        pattern(
          compile("[_$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*(?=\\s*=\\s*(?:function\\b|(?:\\([^()]*\\)|[_$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*)\\s*=>))", CASE_INSENSITIVE),
          false,
          false,
          "function"
        )
      ),
      token("constant", pattern(compile("\\b[A-Z][A-Z\\d_]*\\b")))
    );

    final Prism4j.Token interpolation = token("interpolation");

    GrammarUtils.insertBeforeToken(js, "string",
      token(
        "template-string",
        pattern(
          compile("`(?:\\\\[\\s\\S]|\\$\\{[^}]+\\}|[^\\\\`])*`"),
          false,
          true,
          null,
          grammar(
            "inside",
            interpolation,
            token("string", pattern(compile("[\\s\\S]+")))
          )
        )
      )
    );

    final Prism4j.Grammar insideInterpolation;
    {
      final List<Prism4j.Token> tokens = new ArrayList<>(js.tokens().size() + 1);
      tokens.add(token(
        "interpolation-punctuation",
        pattern(compile("^\\$\\{|\\}$"), false, false, "punctuation")
      ));
      tokens.addAll(js.tokens());
      insideInterpolation = grammar("inside", tokens);
    }

    interpolation.patterns().add(pattern(
      compile("\\$\\{[^}]+\\}"),
      false,
      false,
      null,
      insideInterpolation
    ));

    final Prism4j.Grammar markup = prism4j.grammar("markup");
    if (markup != null) {
      GrammarUtils.insertBeforeToken(markup, "tag",
        token(
          "script", pattern(
            compile("(<script[\\s\\S]*?>)[\\s\\S]*?(?=<\\/script>)", CASE_INSENSITIVE),
            true,
            true,
            "language-javascript",
            js
          )
        )
      );
    }

    return js;
  }
}
