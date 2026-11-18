/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: Copyright 2019 Dimitry Ivanov (legal@noties.io)
 * SPDX-License-Identifier: Apache-2.0
 */
package thirdparties.io.noties.prism4j.languages

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.GrammarUtils
import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.grammar
import io.noties.prism4j.Prism4j.pattern
import io.noties.prism4j.Prism4j.token
import java.util.regex.Pattern.CASE_INSENSITIVE
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

@Suppress("LargeClass")
class MarkwonGrammarLocator : GrammarLocator {
    private val cache = HashMap<String, Prism4j.Grammar>()

    private val grammarFactories: Map<String, (Prism4j) -> Prism4j.Grammar> = mapOf(
        "c" to { createC(it) },
        "clike" to { createCLike() },
        "clojure" to { createClojure() },
        "cpp" to { createCpp(it) },
        "csharp" to { createCSharp(it) },
        CSS to { createCss(it) },
        "dart" to { createDart(it) },
        "git" to { createGit() },
        "go" to { createGo(it) },
        "groovy" to { createGroovy(it) },
        "java" to { createJava(it) },
        JAVASCRIPT to { createJavaScript(it) },
        "json" to { createJson() },
        "kotlin" to { createKotlin(it) },
        "latex" to { createLatex() },
        "makefile" to { createMakefile() },
        "markdown" to { createMarkdown(it) },
        MARKUP to { createMarkup() },
        "python" to { createPython() },
        "scala" to { createScala(it) },
        "sql" to { createSql() },
        "swift" to { createSwift(it) },
        "yaml" to { createYaml() }
    )

    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? {
        val name = ALIASES[language] ?: language
        cache[name]?.let { return it }

        val factory = grammarFactories[name] ?: return null
        val grammar = factory(prism4j)
        cache[name] = grammar
        triggerModify(prism4j, name)

        return grammar
    }

    override fun languages(): Set<String> = HashSet(grammarFactories.keys)

    private fun triggerModify(prism4j: Prism4j, name: String) {
        if (name != MARKUP) return

        prism4j.grammar(CSS)
        prism4j.grammar(JAVASCRIPT)
    }

    private fun createC(prism4j: Prism4j): Prism4j.Grammar {
        val c = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "c",
            { it.name() != "class-name" && it.name() != "boolean" },
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:_Alignas|_Alignof|_Atomic|_Bool|_Complex|_Generic|_Imaginary|_Noreturn|""" +
                            """_Static_assert|_Thread_local|asm|typeof|inline|auto|break|case|char|const|continue|""" +
                            """default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|""" +
                            """signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while)\b"""
                    )
                )
            ),
            token(
                "operator",
                pattern(compile("""-[>-]?|\+\+?|!=?|<<?=?|>>?=?|==?|&&?|\|\|?|[~^%?*/]"""))
            ),
            token(
                "number",
                pattern(
                    compile(
                        """(?:\b0x[\da-f]+|(?:\b\d+\.?\d*|\B\.\d+)(?:e[+-]?\d+)?)[ful]*""",
                        CASE_INSENSITIVE
                    )
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            c,
            "string",
            token(
                "macro",
                pattern(
                    compile(
                        """(^\s*)#\s*[a-z]+(?:[^\r\n\\]|\\(?:\r\n|[\s\S]))*""",
                        CASE_INSENSITIVE or MULTILINE
                    ),
                    true,
                    false,
                    "property",
                    grammar(
                        "inside",
                        token(
                            "string",
                            pattern(compile("""(#\s*include\s*)(?:<.+?>|(["'])(?:\\?.)+?\2)"""), true)
                        ),
                        token(
                            "directive",
                            pattern(
                                compile(
                                    """(#\s*)\b(?:define|defined|elif|else|endif|error|ifdef|ifndef|if|""" +
                                        """import|include|line|pragma|undef|using)\b"""
                                ),
                                true,
                                false,
                                "keyword"
                            )
                        )
                    )
                )
            ),
            token(
                "constant",
                pattern(
                    compile(
                        """\b(?:__FILE__|__LINE__|__DATE__|__TIME__|__TIMESTAMP__|__func__|EOF|NULL|SEEK_CUR|""" +
                            """SEEK_END|SEEK_SET|stdin|stdout|stderr)\b"""
                    )
                )
            )
        )

        return c
    }

    private fun createCLike(): Prism4j.Grammar = grammar(
        "clike",
        token(
            "comment",
            pattern(compile("""(^|[^\\])/\*[\s\S]*?(?:\*/|$)"""), true),
            pattern(compile("""(^|[^\\:])//.*"""), true, true)
        ),
        token(
            "string",
            pattern(
                compile("""(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1"""),
                false,
                true
            )
        ),
        token(
            "class-name",
            pattern(
                compile("""(\b(?:class|interface|extends|implements|trait|instanceof|new)\s+|catch\s+\()[\w.\\]+"""),
                true,
                false,
                null,
                grammar("inside", token("punctuation", pattern(compile("""[.\\]"""))))
            )
        ),
        token(
            "keyword",
            pattern(
                compile(
                    """\b(?:if|else|while|do|for|return|in|instanceof|function|new|try|throw|catch|finally|""" +
                        """null|break|continue)\b"""
                )
            )
        ),
        token("boolean", pattern(compile("""\b(?:true|false)\b"""))),
        token("function", pattern(compile("""[a-z0-9_]+(?=\()""", CASE_INSENSITIVE))),
        token(
            "number",
            pattern(
                compile(
                    """\b0x[\da-f]+\b|(?:\b\d+\.?\d*|\B\.\d+)(?:e[+-]?\d+)?""",
                    CASE_INSENSITIVE
                )
            )
        ),
        token(
            "operator",
            pattern(compile("""--?|\+\+?|!=?=?|<=?|>=?|==?=?|&&?|\|\|?|\?|\*|/|~|\^|%"""))
        ),
        token("punctuation", pattern(compile("""[{}\[\];(),.:]""")))
    )

    private fun createClojure(): Prism4j.Grammar = grammar(
        "clojure",
        token("comment", pattern(compile(";+.*"))),
        token("string", pattern(compile("\"(?:\\\\.|[^\\\\\"\\r\\n])*\""))),
        token(
            "operator",
            pattern(compile("""(?:::|[:|'])\b[a-z][\w*+!?-]*\b""", CASE_INSENSITIVE))
        ),
        token(
            "keyword",
            pattern(
                compile(
                    """([^\w+*'?-])(?:def|if|do|let|\.\.|quote|var|->>|->|fn|loop|recur|throw|try|""" +
                        """monitor-enter|\.|new|set!|def-|defn|defn-|defmacro|defmulti|defmethod|defstruct|""" +
                        """defonce|declare|definline|definterface|defprotocol|==|defrecord|>=|deftype|<=|""" +
                        """defproject|ns|\*|\+|-|/|<|=|>|accessor|agent|agent-errors|aget|alength|all-ns|""" +
                        """alter|and|append-child|apply|array-map|aset|aset-boolean|aset-byte|aset-char|""" +
                        """aset-double|aset-float|aset-int|aset-long|aset-short|assert|assoc|await|""" +
                        """await-for|bean|binding|bit-and|bit-not|bit-or|bit-shift-left|bit-shift-right|""" +
                        """bit-xor|boolean|branch\?|butlast|byte|cast|char|children|class|""" +
                        """clear-agent-errors|comment|commute|comp|comparator|complement|concat|conj|cons|""" +
                        """constantly|cond|if-not|construct-proxy|contains\?|count|create-ns|create-struct|""" +
                        """cycle|dec|deref|difference|disj|dissoc|distinct|doall|doc|dorun|doseq|dosync|""" +
                        """dotimes|doto|double|down|drop|drop-while|edit|end\?|ensure|eval|every\?|false\?|""" +
                        """ffirst|file-seq|filter|find|find-doc|find-ns|find-var|first|float|flush|for|""" +
                        """fnseq|frest|gensym|get-proxy-class|get|hash-map|hash-set|identical\?|identity|""" +
                        """if-let|import|in-ns|inc|index|insert-child|insert-left|insert-right|""" +
                        """inspect-table|inspect-tree|instance\?|int|interleave|intersection|into|""" +
                        """into-array|iterate|join|key|keys|keyword|keyword\?|last|lazy-cat|lazy-cons|left|""" +
                        """lefts|line-seq|list\*|list|load|load-file|locking|long|macroexpand|""" +
                        """macroexpand-1|make-array|make-node|map|map-invert|map\?|mapcat|max|max-key|""" +
                        """memfn|merge|merge-with|meta|min|min-key|name|namespace|neg\?|newline|next|nil\?|""" +
                        """node|not|not-any\?|not-every\?|not=|ns-imports|ns-interns|ns-map|ns-name|""" +
                        """ns-publics|ns-refers|ns-resolve|ns-unmap|nth|nthrest|or|parse|partial|path|peek|""" +
                        """pop|pos\?|pr|pr-str|print|print-str|println|println-str|prn|prn-str|project|""" +
                        """proxy|proxy-mappings|quot|rand|rand-int|range|re-find|re-groups|re-matcher|""" +
                        """re-matches|re-pattern|re-seq|read|read-line|reduce|ref|ref-set|refer|rem|remove|""" +
                        """remove-method|remove-ns|rename|rename-keys|repeat|replace|replicate|resolve|""" +
                        """rest|resultset-seq|reverse|rfirst|right|rights|root|rrest|rseq|second|select|""" +
                        """select-keys|send|send-off|seq|seq-zip|seq\?|set|short|slurp|some|sort|sort-by|""" +
                        """sorted-map|sorted-map-by|sorted-set|special-symbol\?|split-at|split-with|str|""" +
                        """string\?|struct|struct-map|subs|subvec|symbol|symbol\?|sync|take|take-nth|""" +
                        """take-while|test|time|to-array|to-array-2d|tree-seq|true\?|union|up|update-proxy|""" +
                        """val|vals|var-get|var-set|var\?|vector|vector-zip|vector\?|when|when-first|""" +
                        """when-let|when-not|with-local-vars|with-meta|with-open|with-out-str|xml-seq|""" +
                        """xml-zip|zero\?|zipmap|zipper)(?=[^\w+*'?-])"""
                ),
                true
            )
        ),
        token("boolean", pattern(compile("""\b(?:true|false|nil)\b"""))),
        token("number", pattern(compile("""\b[0-9A-Fa-f]+\b"""))),
        token("punctuation", pattern(compile("""[{}\[\](),]""")))
    )

    private fun createCpp(prism4j: Prism4j): Prism4j.Grammar {
        val cpp = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "c"),
            "cpp",
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:alignas|alignof|asm|auto|bool|break|case|catch|char|char16_t|char32_t|class|""" +
                            """compl|const|constexpr|const_cast|continue|decltype|default|delete|do|double|""" +
                            """dynamic_cast|else|enum|explicit|export|extern|float|for|friend|goto|if|inline|int|""" +
                            """int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t|long|mutable|""" +
                            """namespace|new|noexcept|nullptr|operator|private|protected|public|register|""" +
                            """reinterpret_cast|return|short|signed|sizeof|static|static_assert|static_cast|struct|""" +
                            """switch|template|this|thread_local|throw|try|typedef|typeid|typename|union|unsigned|""" +
                            """using|virtual|void|volatile|wchar_t|while)\b"""
                    )
                )
            ),
            token(
                "operator",
                pattern(
                    compile(
                        """--?|\+\+?|!=?|<{1,2}=?|>{1,2}=?|->|:{1,2}|={1,2}|\^|~|%|&{1,2}|\|\|?|\?|\*|/|""" +
                            """\b(?:and|and_eq|bitand|bitor|not|not_eq|or|or_eq|xor|xor_eq)\b"""
                    )
                )
            )
        )

        // in prism-js cpp is extending c, but c has not booleans... (like classes)
        GrammarUtils.insertBeforeToken(
            cpp,
            "function",
            token("boolean", pattern(compile("""\b(?:true|false)\b""")))
        )

        GrammarUtils.insertBeforeToken(
            cpp,
            "keyword",
            token(
                "class-name",
                pattern(compile("""(class\s+)\w+""", CASE_INSENSITIVE), true)
            )
        )

        GrammarUtils.insertBeforeToken(
            cpp,
            "string",
            token(
                "raw-string",
                pattern(compile("R\"([^()\\\\ ]{0,16})\\([\\s\\S]*?\\)\\1\""), false, true, "string")
            )
        )

        return cpp
    }

    private fun createCSharp(prism4j: Prism4j): Prism4j.Grammar {
        val classNameInsidePunctuation = grammar(
            "inside",
            token("punctuation", pattern(compile("""\.""")))
        )

        val keyword = token(
            "keyword",
            pattern(
                compile(
                    """\b(?:abstract|add|alias|as|ascending|async|await|base|bool|break|byte|case|catch|char|""" +
                        """checked|class|const|continue|decimal|default|delegate|descending|do|double|dynamic|else|""" +
                        """enum|event|explicit|extern|false|finally|fixed|float|for|foreach|from|get|global|goto|""" +
                        """group|if|implicit|in|int|interface|internal|into|is|join|let|lock|long|namespace|new|""" +
                        """null|object|operator|orderby|out|override|params|partial|private|protected|public|""" +
                        """readonly|ref|remove|return|sbyte|sealed|select|set|short|sizeof|stackalloc|static|""" +
                        """string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|""" +
                        """using|value|var|virtual|void|volatile|where|while|yield)\b"""
                )
            )
        )

        val csharp = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "csharp",
            keyword,
            token(
                "string",
                pattern(compile("""@(["'])(?:\1\1|\\[\s\S]|(?!\1)[^\\])*\1"""), false, true),
                pattern(compile("""(["'])(?:\\.|(?!\1)[^\\\r\n])*?\1"""), false, true)
            ),
            token(
                "class-name",
                pattern(
                    compile("""\b[A-Z]\w*(?:\.\w+)*\b(?=\s+\w+)"""),
                    false,
                    false,
                    null,
                    classNameInsidePunctuation
                ),
                pattern(
                    compile("""(\[)[A-Z]\w*(?:\.\w+)*\b"""),
                    true,
                    false,
                    null,
                    classNameInsidePunctuation
                ),
                pattern(
                    compile("""(\b(?:class|interface)\s+[A-Z]\w*(?:\.\w+)*\s*:\s*)[A-Z]\w*(?:\.\w+)*\b"""),
                    true,
                    false,
                    null,
                    classNameInsidePunctuation
                ),
                pattern(
                    compile("""(\b(?:class|interface|new)\s+|catch\s+\()[A-Z]\w*(?:\.\w+)*\b"""),
                    true,
                    false,
                    null,
                    classNameInsidePunctuation
                )
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b0x[\da-f]+\b|(?:\b\d+\.?\d*|\B\.\d+)f?""",
                        CASE_INSENSITIVE
                    )
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            csharp,
            "class-name",
            createCSharpGenericMethod(keyword, classNameInsidePunctuation),
            token(
                "preprocessor",
                pattern(
                    compile("""(^\s*)#.*""", MULTILINE),
                    true,
                    false,
                    "property",
                    grammar(
                        "inside",
                        token(
                            "directive",
                            pattern(
                                compile(
                                    """(\s*#)\b(?:define|elif|else|endif|endregion|error|if|line|pragma|""" +
                                        """region|undef|warning)\b"""
                                ),
                                true,
                                false,
                                "keyword"
                            )
                        )
                    )
                )
            )
        )

        return csharp
    }

    private fun createCSharpGenericMethod(
        keyword: Prism4j.Token,
        classNameInsidePunctuation: Prism4j.Grammar
    ): Prism4j.Token = token(
        "generic-method",
        pattern(
            compile("""\w+\s*<[^>\r\n]+?>\s*(?=\()"""),
            false,
            false,
            null,
            grammar(
                "inside",
                token("function", pattern(compile("""^\w+"""))),
                token(
                    "class-name",
                    pattern(
                        compile("""\b[A-Z]\w*(?:\.\w+)*\b"""),
                        false,
                        false,
                        null,
                        classNameInsidePunctuation
                    )
                ),
                keyword,
                token("punctuation", pattern(compile("[<>(),.:]")))
            )
        )
    )

    private fun createCss(prism4j: Prism4j): Prism4j.Grammar {
        val atruleInside = grammar(
            "inside",
            token("rule", pattern(compile("""@[\w-]+""")))
        )

        val css = grammar(
            "css",
            token("comment", pattern(compile("""/\*[\s\S]*?\*/"""))),
            token(
                "atrule",
                pattern(
                    compile("""@[\w-]+?.*?(?:;|(?=\s*\{))""", CASE_INSENSITIVE),
                    false,
                    false,
                    null,
                    atruleInside
                )
            ),
            token(
                "url",
                pattern(
                    compile(
                        """url\((?:(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1|.*?)\)""",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token("selector", pattern(compile("""[^{}\s][^{};]*?(?=\s*\{)"""))),
            token(
                "string",
                pattern(
                    compile("""(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1"""),
                    false,
                    true
                )
            ),
            token(
                "property",
                pattern(
                    compile(
                        """[-_a-z\xA0-\uFFFF][-\w\xA0-\uFFFF]*(?=\s*:)""",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token("important", pattern(compile("""\B!important\b""", CASE_INSENSITIVE))),
            token("function", pattern(compile("""[-a-z0-9]+(?=\()""", CASE_INSENSITIVE))),
            token("punctuation", pattern(compile("[(){};:]")))
        )

        // can we maybe add some helper to specify simplified location?

        // now we need to put the all tokens from grammar inside `atrule` (except the `atrule` of cause)
        atruleInside.tokens().addAll(css.tokens().filter { it.name() != "atrule" })

        prism4j.grammar("markup")?.let { insertCssIntoMarkup(it, css) }

        return css
    }

    private fun insertCssIntoMarkup(markup: Prism4j.Grammar, css: Prism4j.Grammar) {
        GrammarUtils.insertBeforeToken(
            markup,
            "tag",
            token(
                "style",
                pattern(
                    compile("""(<style[\s\S]*?>)[\s\S]*?(?=</style>)""", CASE_INSENSITIVE),
                    true,
                    true,
                    "language-css",
                    css
                )
            )
        )

        // important thing here is to clone found grammar
        // otherwise we will have stackoverflow (inside tag references style-attr, which
        // references inside tag, etc)
        val markupTagInside = GrammarUtils.findToken(markup, "tag")
            ?.let { GrammarUtils.findFirstInsideGrammar(it) }
            ?.let { GrammarUtils.clone(it) }

        GrammarUtils.insertBeforeToken(
            markup,
            "tag/attr-value",
            token(
                "style-attr",
                pattern(
                    compile(
                        """\s*style=(["'])(?:\\[\s\S]|(?!\1)[^\\])*\1""",
                        CASE_INSENSITIVE
                    ),
                    false,
                    false,
                    "language-css",
                    grammar(
                        "inside",
                        token(
                            "attr-name",
                            pattern(
                                compile("""^\s*style""", CASE_INSENSITIVE),
                                false,
                                false,
                                null,
                                markupTagInside
                            )
                        ),
                        token(
                            "punctuation",
                            pattern(compile("""^\s*=\s*['"]|['"]\s*$"""))
                        ),
                        token(
                            "attr-value",
                            pattern(
                                compile(".+", CASE_INSENSITIVE),
                                false,
                                false,
                                null,
                                css
                            )
                        )
                    )
                )
            )
        )
    }

    private fun createDart(prism4j: Prism4j): Prism4j.Grammar {
        val dart = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "dart",
            token(
                "string",
                pattern(compile("r?(\"\"\"|''')[\\s\\S]*?\\1"), false, true),
                pattern(compile("""r?(["'])(?:\\.|(?!\1)[^\\\r\n])*\1"""), false, true)
            ),
            token(
                "keyword",
                pattern(compile("""\b(?:async|sync|yield)\*""")),
                pattern(
                    compile(
                        """\b(?:abstract|assert|async|await|break|case|catch|class|const|continue|default|""" +
                            """deferred|do|dynamic|else|enum|export|external|extends|factory|final|finally|for|get|""" +
                            """if|implements|import|in|library|new|null|operator|part|rethrow|return|set|static|""" +
                            """super|switch|this|throw|try|typedef|var|void|while|with|yield)\b"""
                    )
                )
            ),
            token(
                "operator",
                pattern(compile("""\bis!|\b(?:as|is)\b|\+\+|--|&&|\|\||<<=?|>>=?|~(?:/=?)?|[+\-*/%&^|=!<>]=?|\?"""))
            )
        )

        GrammarUtils.insertBeforeToken(
            dart,
            "function",
            token("metadata", pattern(compile("""@\w+"""), false, false, "symbol"))
        )

        return dart
    }

    private fun createGit(): Prism4j.Grammar = grammar(
        "git",
        token("comment", pattern(compile("^#.*", MULTILINE))),
        token("deleted", pattern(compile("^[-–].*", MULTILINE))),
        token("inserted", pattern(compile("""^\+.*""", MULTILINE))),
        token(
            "string",
            pattern(compile("""(["'])(?:\\.|(?!\1)[^\\\r\n])*\1""", MULTILINE))
        ),
        token(
            "command",
            pattern(
                compile("""^.*\$ git .*$""", MULTILINE),
                false,
                false,
                null,
                grammar(
                    "inside",
                    token("parameter", pattern(compile("""\s--?\w+""", MULTILINE)))
                )
            )
        ),
        token("coord", pattern(compile("^@@.*@@$", MULTILINE))),
        token("commit_sha1", pattern(compile("""^commit \w{40}$""", MULTILINE)))
    )

    private fun createGo(prism4j: Prism4j): Prism4j.Grammar {
        val go = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "go",
            { it.name() != "class-name" },
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:break|case|chan|const|continue|default|defer|else|fallthrough|for|func|""" +
                            """go(?:to)?|if|import|interface|map|package|range|return|select|struct|switch|type|""" +
                            """var)\b"""
                    )
                )
            ),
            token("boolean", pattern(compile("""\b(?:_|iota|nil|true|false)\b"""))),
            token(
                "operator",
                pattern(
                    compile("""[*/%^!=]=?|\+[=+]?|-[=-]?|\|[=|]?|&(?:=|&|\^=?)?|>(?:>=?|=)?|<(?:<=?|=|-)?|:=|\.\.\.""")
                )
            ),
            token(
                "number",
                pattern(
                    compile(
                        """(?:\b0x[a-f\d]+|(?:\b\d+\.?\d*|\B\.\d+)(?:e[-+]?\d+)?)i?""",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token(
                "string",
                pattern(
                    compile("""(["'`])(\\[\s\S]|(?!\1)[^\\])*\1"""),
                    false,
                    true
                )
            )
        )

        // clike doesn't have builtin
        GrammarUtils.insertBeforeToken(
            go,
            "boolean",
            token(
                "builtin",
                pattern(
                    compile(
                        """\b(?:bool|byte|complex(?:64|128)|error|float(?:32|64)|rune|string|u?int(?:8|16|32|""" +
                            """64)?|uintptr|append|cap|close|complex|copy|delete|imag|len|make|new|panic|""" +
                            """print(?:ln)?|real|recover)\b"""
                    )
                )
            )
        )

        return go
    }

    private fun createGroovy(prism4j: Prism4j): Prism4j.Grammar {
        val groovy = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "groovy",
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:as|def|in|abstract|assert|boolean|break|byte|case|catch|char|class|const|""" +
                            """continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|""" +
                            """implements|import|instanceof|int|interface|long|native|new|package|private|""" +
                            """protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|""" +
                            """throws|trait|transient|try|void|volatile|while)\b"""
                    )
                )
            ),
            token(
                "string",
                pattern(
                    compile("(\"\"\"|''')[\\s\\S]*?\\1|\\$/(?:\\$/\\$|[\\s\\S])*?/\\$"),
                    false,
                    true
                ),
                pattern(
                    compile("""(["'/])(?:\\.|(?!\1)[^\\\r\n])*\1"""),
                    false,
                    true
                )
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b(?:0b[01_]+|0x[\da-f_]+(?:\.[\da-f_p\-]+)?|[\d_]+(?:\.[\d_]+)?(?:e[+-]?\d+)?)[glidf]?\b""",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token(
                "operator",
                pattern(
                    compile(
                        """(^|[^.])(?:~|==?~?|\?[.:]?|\*(?:[.=]|\*=?)?|\.[@&]|\.\.<|\.{1,2}(?!\.)|-[-=>]?|""" +
                            """\+[+=]?|!=?|<(?:<=?|=>?)?|>(?:>>?=?|=)?|&[&=]?|\|[|=]?|/=?|\^=?|%=?)"""
                    ),
                    true
                )
            ),
            token(
                "punctuation",
                pattern(compile("""\.+|[{}\[\];(),:$]"""))
            )
        )

        GrammarUtils.insertBeforeToken(
            groovy,
            "string",
            token(
                "shebang",
                pattern(
                    compile("#!.+"),
                    false,
                    false,
                    "comment"
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            groovy,
            "punctuation",
            token(
                "spock-block",
                pattern(
                    compile("""\b(?:setup|given|when|then|and|cleanup|expect|where):""")
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            groovy,
            "function",
            token(
                "annotation",
                pattern(
                    compile("""(^|[^.])@\w+"""),
                    true,
                    false,
                    "punctuation"
                )
            )
        )

        // no string templates :(
        return groovy
    }

    private fun createJava(prism4j: Prism4j): Prism4j.Grammar {
        val keyword = token(
            "keyword",
            pattern(
                compile(
                    """\b(?:abstract|continue|for|new|switch|assert|default|goto|package|synchronized|boolean|""" +
                        """do|if|private|this|break|double|implements|protected|throw|byte|else|import|public|""" +
                        """throws|case|enum|instanceof|return|transient|catch|extends|int|short|try|char|final|""" +
                        """interface|static|void|class|finally|long|strictfp|volatile|const|float|native|super|""" +
                        """while)\b"""
                )
            )
        )

        val java = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "java",
            keyword,
            token(
                "number",
                pattern(
                    compile(
                        """\b0b[01]+\b|\b0x[\da-f]*\.?[\da-fp-]+\b|(?:\b\d+\.?\d*|\B\.\d+)(?:e[+-]?\d+)?[df]?""",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token(
                "operator",
                pattern(
                    compile(
                        """(^|[^.])(?:\+[+=]?|-[-=]?|!=?|<<?=?|>>?>?=?|==?|&[&=]?|\|[|=]?|\*=?|/=?|%=?|\^=?|[?:~])""",
                        MULTILINE
                    ),
                    true
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            java,
            "function",
            token(
                "annotation",
                pattern(
                    compile("""(^|[^.])@\w+"""),
                    true,
                    false,
                    "punctuation"
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            java,
            "class-name",
            token(
                "generics",
                pattern(
                    compile(
                        """<\s*\w+(?:\.\w+)?(?:\s*,\s*\w+(?:\.\w+)?)*>""",
                        CASE_INSENSITIVE
                    ),
                    false,
                    false,
                    "function",
                    grammar(
                        "inside",
                        keyword,
                        token("punctuation", pattern(compile("[<>(),.:]")))
                    )
                )
            )
        )

        return java
    }

    private fun createJavaScript(prism4j: Prism4j): Prism4j.Grammar {
        val js = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "javascript",
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:as|async|await|break|case|catch|class|const|continue|debugger|default|delete|""" +
                            """do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|""" +
                            """instanceof|interface|let|new|null|of|package|private|protected|public|return|set|""" +
                            """static|super|switch|this|throw|try|typeof|var|void|while|with|yield)\b"""
                    )
                )
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b(?:0[xX][\dA-Fa-f]+|0[bB][01]+|0[oO][0-7]+|NaN|Infinity)\b|(?:\b\d+\.?\d*|""" +
                            """\B\.\d+)(?:[Ee][+-]?\d+)?"""
                    )
                )
            ),
            token(
                "function",
                pattern(
                    compile(
                        "[_\$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*(?=\\s*\\()",
                        CASE_INSENSITIVE
                    )
                )
            ),
            token(
                "operator",
                pattern(
                    compile(
                        """-[-=]?|\+[+=]?|!=?=?|<<?=?|>>?>?=?|=(?:==?|>)?|&[&=]?|\|[|=]?|\*\*?=?|/=?|~|\^=?|""" +
                            """%=?|\?|\.{3}"""
                    )
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            js,
            "keyword",
            token(
                "regex",
                pattern(
                    compile(
                        """((?:^|[^$\w\xA0-\uFFFF."'\])\s])\s*)/(\[[^\]\r\n]+]|\\.|""" +
                            """[^/\\\[\r\n])+/[gimyu]{0,5}(?=\s*($|[\r\n,.;})\]]))"""
                    ),
                    true,
                    true
                )
            ),
            token(
                "function-variable",
                pattern(
                    compile(
                        "[_\$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*(?=\\s*=\\s*(?:function\\b|" +
                            "(?:\\([^()]*\\)|[_\$a-z\\xA0-\\uFFFF][$\\w\\xA0-\\uFFFF]*)\\s*=>))",
                        CASE_INSENSITIVE
                    ),
                    false,
                    false,
                    "function"
                )
            ),
            token("constant", pattern(compile("""\b[A-Z][A-Z\d_]*\b""")))
        )

        val interpolation = token("interpolation")

        GrammarUtils.insertBeforeToken(
            js,
            "string",
            token(
                "template-string",
                pattern(
                    compile("""`(?:\\[\s\S]|\$\{[^}]+\}|[^\\`])*`"""),
                    false,
                    true,
                    null,
                    grammar(
                        "inside",
                        interpolation,
                        token("string", pattern(compile("""[\s\S]+""")))
                    )
                )
            )
        )

        val insideInterpolationTokens = mutableListOf(
            token(
                "interpolation-punctuation",
                pattern(compile("""^\$\{|\}$"""), false, false, "punctuation")
            )
        )
        insideInterpolationTokens.addAll(js.tokens())

        interpolation.patterns().add(
            pattern(
                compile("""\$\{[^}]+\}"""),
                false,
                false,
                null,
                grammar("inside", insideInterpolationTokens)
            )
        )

        prism4j.grammar("markup")?.let { insertJavaScriptIntoMarkup(it, js) }

        return js
    }

    private fun insertJavaScriptIntoMarkup(markup: Prism4j.Grammar, js: Prism4j.Grammar) {
        GrammarUtils.insertBeforeToken(
            markup,
            "tag",
            token(
                "script",
                pattern(
                    compile("""(<script[\s\S]*?>)[\s\S]*?(?=</script>)""", CASE_INSENSITIVE),
                    true,
                    true,
                    "language-javascript",
                    js
                )
            )
        )
    }

    private fun createJson(): Prism4j.Grammar = grammar(
        "json",
        token(
            "property",
            pattern(compile("\"(?:\\\\.|[^\\\\\"\\r\\n])*\"(?=\\s*:)", CASE_INSENSITIVE))
        ),
        token(
            "string",
            pattern(compile("\"(?:\\\\.|[^\\\\\"\\r\\n])*\"(?!\\s*:)"), false, true)
        ),
        token(
            "number",
            pattern(compile("""\b0x[\dA-Fa-f]+\b|(?:\b\d+\.?\d*|\B\.\d+)(?:[Ee][+-]?\d+)?"""))
        ),
        token(
            "punctuation",
            pattern(compile("""[{}\[\]);,]"""))
        ), // not sure about this one...
        token("operator", pattern(compile(":"))),
        token(
            "boolean",
            pattern(compile("""\b(?:true|false)\b""", CASE_INSENSITIVE))
        ),
        token("null", pattern(compile("""\bnull\b""", CASE_INSENSITIVE)))
    )

    private fun createKotlin(prism4j: Prism4j): Prism4j.Grammar {
        val kotlin = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "kotlin",
            { it.name() != "class-name" },
            token(
                "keyword",
                pattern(
                    compile(
                        """(^|[^.])\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|""" +
                            """constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|""" +
                            """final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|""" +
                            """internal|is|lateinit|noinline|null|object|open|operator|out|override|package|""" +
                            """private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|""" +
                            """throw|to|try|typealias|val|var|vararg|when|where|while)\b"""
                    ),
                    true
                )
            ),
            token(
                "function",
                pattern(compile("""\w+(?=\s*\()""")),
                pattern(compile("""(\.)\w+(?=\s*\{)"""), true)
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b(?:0[xX][\da-fA-F]+(?:_[\da-fA-F]+)*|0[bB][01]+(?:_[01]+)*|""" +
                            """\d+(?:_\d+)*(?:\.\d+(?:_\d+)*)?(?:[eE][+-]?\d+(?:_\d+)*)?[fFL]?)\b"""
                    )
                )
            ),
            token(
                "operator",
                pattern(
                    compile(
                        """\+[+=]?|-[-=>]?|==?=?|!(?:!|==?)?|[/*%<>]=?|[?:]:?|\.\.|&&|\|\||\b(?:and|inv|or|shl|""" +
                            """shr|ushr|xor)\b"""
                    )
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            kotlin,
            "string",
            token(
                "raw-string",
                pattern(compile("(\"\"\"|''')[\\s\\S]*?\\1"), false, false, "string")
            )
        )

        GrammarUtils.insertBeforeToken(
            kotlin,
            "keyword",
            token(
                "annotation",
                pattern(compile("""\B@(?:\w+:)?(?:[A-Z]\w*|\[[^\]]+\])"""), false, false, "builtin")
            )
        )

        GrammarUtils.insertBeforeToken(
            kotlin,
            "function",
            token("label", pattern(compile("""\w+@|@\w+"""), false, false, "symbol"))
        )

        // okay, I was cloning the tokens of kotlin grammar (so there is no recursive chain of calls),
        // but it looks like it wants to have recursive calls
        // I did this because interpolation test was failing due to the fact that `string`
        // `raw-string` tokens didn't have `inside`, so there were not tokenized
        // I still find that it has potential to fall with stackoverflow (in some cases)
        val interpolationTokens = mutableListOf(
            token(
                "delimiter",
                pattern(compile("""^\$\{|\}$"""), false, false, "variable")
            )
        )
        interpolationTokens.addAll(kotlin.tokens())

        // this grammar has 1 token: interpolation, which has 2 patterns
        val interpolationInside = grammar(
            "inside",
            token(
                "interpolation",
                pattern(
                    compile("""\$\{[^}]+\}"""),
                    false,
                    false,
                    null,
                    grammar("inside", interpolationTokens)
                ),
                pattern(compile("""\$\w+"""), false, false, "variable")
            )
        )

        val string = GrammarUtils.findToken(kotlin, "string")
        val rawString = GrammarUtils.findToken(kotlin, "raw-string")
        if (string == null || rawString == null) {
            throw RuntimeException(
                "Unexpected state, cannot find `string` and/or `raw-string` tokens " +
                    "inside kotlin grammar"
            )
        }

        string.replaceFirstPatternInside(interpolationInside)
        rawString.replaceFirstPatternInside(interpolationInside)

        return kotlin
    }

    private fun Prism4j.Token.replaceFirstPatternInside(inside: Prism4j.Grammar) {
        val patterns = patterns()
        val first = patterns[0]
        patterns.add(pattern(first.regex(), first.lookbehind(), first.greedy(), first.alias(), inside))

        patterns.removeAt(0)
    }

    private fun createLatex(): Prism4j.Grammar {
        val funcPattern = compile("""\\(?:[^a-z()\[\]]|[a-z*]+)""", CASE_INSENSITIVE)

        val insideEqu = grammar(
            "inside",
            token("equation-command", pattern(funcPattern, false, false, "regex"))
        )

        return grammar(
            "latex",
            token("comment", pattern(compile("%.*", MULTILINE))),
            token(
                "cdata",
                pattern(
                    compile("""(\\begin\{((?:verbatim|lstlisting)\*?)\})[\s\S]*?(?=\\end\{\2\})"""),
                    true
                )
            ),
            token(
                "equation",
                pattern(
                    compile("""\$(?:\\[\s\S]|[^\\$])*\$|\\\([\s\S]*?\\\)|\\\[[\s\S]*?\\\]"""),
                    false,
                    false,
                    "string",
                    insideEqu
                ),
                pattern(
                    compile(
                        """(\\begin\{((?:equation|math|eqnarray|align|multline|""" +
                            """gather)\*?)\})[\s\S]*?(?=\\end\{\2\})"""
                    ),
                    true,
                    false,
                    "string",
                    insideEqu
                )
            ),
            token(
                "keyword",
                pattern(
                    compile(
                        """(\\(?:begin|end|ref|cite|label|usepackage|""" +
                            """documentclass)(?:\[[^\]]+\])?\{)[^}]+(?=\})"""
                    ),
                    true
                )
            ),
            token(
                "url",
                pattern(
                    compile("""(\\url\{)[^}]+(?=\})"""),
                    true
                )
            ),
            token(
                "headline",
                pattern(
                    compile(
                        """(\\(?:part|chapter|section|subsection|frametitle|subsubsection|paragraph|""" +
                            """subparagraph|subsubparagraph|""" +
                            """subsubsubparagraph)\*?(?:\[[^\]]+\])?\{)[^}]+(?=\}(?:\[[^\]]+\])?)"""
                    ),
                    true,
                    false,
                    "class-name"
                )
            ),
            token(
                "function",
                pattern(
                    funcPattern,
                    false,
                    false,
                    "selector"
                )
            ),
            token("punctuation", pattern(compile("""[\[\]{}&]""")))
        )
    }

    private fun createMakefile(): Prism4j.Grammar = grammar(
        "makefile",
        token(
            "comment",
            pattern(
                compile("""(^|[^\\])#(?:\\(?:\r\n|[\s\S])|[^\\\r\n])*"""),
                true
            )
        ),
        token(
            "string",
            pattern(
                compile("""(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1"""),
                false,
                true
            )
        ),
        token("builtin", pattern(compile("""\.[A-Z][^:#=\s]+(?=\s*:(?!=))"""))),
        token(
            "symbol",
            pattern(
                compile("""^[^:=\r\n]+(?=\s*:(?!=))""", MULTILINE),
                false,
                false,
                null,
                grammar(
                    "inside",
                    token("variable", pattern(compile("""\$+(?:[^(){}:#=\s]+|(?=[({]))""")))
                )
            )
        ),
        token(
            "variable",
            pattern(compile("""\$+(?:[^(){}:#=\s]+|\([@*%<^+?][DF]\)|(?=[({]))"""))
        ),
        token(
            "keyword",
            pattern(
                compile(
                    """-include\b|\b(?:define|else|endef|endif|export|ifn?def|ifn?eq|include|override|private|""" +
                        """sinclude|undefine|unexport|vpath)\b"""
                )
            ),
            pattern(
                compile(
                    """(\()(?:addsuffix|abspath|and|basename|call|dir|error|eval|file|filter(?:-out)?|""" +
                        """findstring|firstword|flavor|foreach|guile|if|info|join|lastword|load|notdir|or|""" +
                        """origin|patsubst|realpath|shell|sort|strip|subst|suffix|value|warning|wildcard|""" +
                        """word(?:s|list)?)(?=[ \t])"""
                ),
                true
            )
        ),
        token("operator", pattern(compile("(?:::|[?:+!])?=|[|@]"))),
        token("punctuation", pattern(compile("[:;(){}]")))
    )

    private fun createMarkdown(prism4j: Prism4j): Prism4j.Grammar {
        val markdown = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "markup"),
            "markdown"
        )

        val boldInside = grammar(
            "inside",
            token("punctuation", pattern(compile("""^\*\*|^__|\*\*$|__$""")))
        )
        val bold = token(
            "bold",
            pattern(
                compile("""(^|[^\\])(\*\*|__)(?:(?:\r?\n|\r)(?!\r?\n|\r)|.)+?\2"""),
                true,
                false,
                null,
                boldInside
            )
        )

        val italicInside = grammar("inside", token("punctuation", pattern(compile("^[*_]|[*_]$"))))
        val italic = token(
            "italic",
            pattern(
                compile("""(^|[^\\])([*_])(?:(?:\r?\n|\r)(?!\r?\n|\r)|.)+?\2"""),
                true,
                false,
                null,
                italicInside
            )
        )

        val url = token(
            "url",
            pattern(
                compile("""!?\[[^\]]+\](?:\([^\s)]+(?:[\t ]+"(?:\\.|[^"\\])*")?\)| ?\[[^\]\n]*\])"""),
                false,
                false,
                null,
                grammar(
                    "inside",
                    token("variable", pattern(compile("""(!?\[)[^\]]+(?=\]$)"""), true)),
                    token("string", pattern(compile("\"(?:\\\\.|[^\"\\\\])*\"(?=\\)$)")))
                )
            )
        )

        GrammarUtils.insertBeforeToken(
            markdown, "prolog",
            token("blockquote", pattern(compile("""^>(?:[\t ]*>)*""", MULTILINE))),
            token(
                "code",
                pattern(compile("""^(?: {4}|\t).+""", MULTILINE), false, false, "keyword"),
                pattern(compile("""``.+?``|`[^`\n]+`"""), false, false, "keyword")
            ),
            token(
                "title",
                pattern(
                    compile("""\w+.*(?:\r?\n|\r)(?:==+|--+)"""),
                    false,
                    false,
                    "important",
                    grammar(
                        "inside",
                        token("punctuation", pattern(compile("==+$|--+$")))
                    )
                ),
                pattern(
                    compile("""(^\s*)#+.+""", MULTILINE),
                    true,
                    false,
                    "important",
                    grammar("inside", token("punctuation", pattern(compile("^#+|#+$"))))
                )
            ),
            token(
                "hr",
                pattern(
                    compile("""(^\s*)([*-])(?:[\t ]*\2){2,}(?=\s*$)""", MULTILINE),
                    true,
                    false,
                    "punctuation"
                )
            ),
            token(
                "list",
                pattern(
                    compile("""(^\s*)(?:[*+-]|\d+\.)(?=[\t ].)""", MULTILINE),
                    true,
                    false,
                    "punctuation"
                )
            ),
            token(
                "url-reference",
                pattern(
                    compile(
                        """!?\[[^\]]+\]:[\t ]+(?:\S+|<(?:\\.|[^>\\])+>)(?:[\t ]+(?:"(?:\\.|[^"\\])*"|""" +
                            """'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\)))?"""
                    ),
                    false,
                    false,
                    "url",
                    grammar(
                        "inside",
                        token("variable", pattern(compile("""^(!?\[)[^\]]+"""), true)),
                        token(
                            "string",
                            pattern(compile("""(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\))$"""))
                        ),
                        token("punctuation", pattern(compile("""^[\[\]!:]|[<>]""")))
                    )
                )
            ),
            bold,
            italic,
            url
        )

        boldInside.tokens().addAll(listOf(url, italic))
        italicInside.tokens().addAll(listOf(url, bold))

        return markdown
    }

    private fun createMarkup(): Prism4j.Grammar {
        val entity = token("entity", pattern(compile("""&#?[\da-z]{1,8};""", CASE_INSENSITIVE)))

        return grammar(
            "markup",
            token("comment", pattern(compile("""<!--[\s\S]*?-->"""))),
            token("prolog", pattern(compile("""<\?[\s\S]+?\?>"""))),
            token(
                "doctype",
                pattern(compile("""<!DOCTYPE[\s\S]+?>""", CASE_INSENSITIVE))
            ),
            token(
                "cdata",
                pattern(compile("""<!\[CDATA\[[\s\S]*?]]>""", CASE_INSENSITIVE))
            ),
            token(
                "tag",
                pattern(
                    compile(
                        """</?(?!\d)[^\s>/=$<%]+(?:\s+[^\s>/=]+(?:=(?:(["'])(?:\\[\s\S]|""" +
                            """(?!\1)[^\\])*\1|[^\s'">=]+))?)*\s*/?>""",
                        CASE_INSENSITIVE
                    ),
                    false,
                    true,
                    null,
                    grammar(
                        "inside",
                        token(
                            "tag",
                            pattern(
                                compile("""^</?[^\s>/]+""", CASE_INSENSITIVE),
                                false,
                                false,
                                null,
                                grammar(
                                    "inside",
                                    token("punctuation", pattern(compile("""^</?"""))),
                                    token("namespace", pattern(compile("""^[^\s>/:]+:""")))
                                )
                            )
                        ),
                        token(
                            "attr-value",
                            pattern(
                                compile(
                                    """=(?:(["'])(?:\\[\s\S]|(?!\1)[^\\])*\1|[^\s'">=]+)""",
                                    CASE_INSENSITIVE
                                ),
                                false,
                                false,
                                null,
                                grammar(
                                    "inside",
                                    token(
                                        "punctuation",
                                        pattern(compile("^=")),
                                        pattern(compile("""(^|[^\\])["']"""), true)
                                    ),
                                    entity
                                )
                            )
                        ),
                        token("punctuation", pattern(compile("""/?>"""))),
                        token(
                            "attr-name",
                            pattern(
                                compile("""[^\s>/]+"""),
                                false,
                                false,
                                null,
                                grammar(
                                    "inside",
                                    token("namespace", pattern(compile("""^[^\s>/:]+:""")))
                                )
                            )
                        )
                    )
                )
            ),
            entity
        )
    }

    private fun createPython(): Prism4j.Grammar = grammar(
        "python",
        token(
            "comment",
            pattern(
                compile("""(^|[^\\])#.*"""),
                true
            )
        ),
        token(
            "triple-quoted-string",
            pattern(
                compile("(\"\"\"|''')[\\s\\S]+?\\1"),
                false,
                true,
                "string"
            )
        ),
        token(
            "string",
            pattern(
                compile("""(["'])(?:\\.|(?!\1)[^\\\r\n])*\1"""),
                false,
                true
            )
        ),
        token(
            "function",
            pattern(
                compile("""((?:^|\s)def[ \t]+)[a-zA-Z_]\w*(?=\s*\()"""),
                true
            )
        ),
        token(
            "class-name",
            pattern(
                compile("""(\bclass\s+)\w+""", CASE_INSENSITIVE),
                true
            )
        ),
        token(
            "keyword",
            pattern(
                compile(
                    """\b(?:as|assert|async|await|break|class|continue|def|del|elif|else|except|exec|finally|""" +
                        """for|from|global|if|import|in|is|lambda|nonlocal|pass|print|raise|return|try|while|with|""" +
                        """yield)\b"""
                )
            )
        ),
        token(
            "builtin",
            pattern(
                compile(
                    """\b(?:__import__|abs|all|any|apply|ascii|basestring|bin|bool|buffer|bytearray|bytes|""" +
                        """callable|chr|classmethod|cmp|coerce|compile|complex|delattr|dict|dir|divmod|enumerate|""" +
                        """eval|execfile|file|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|""" +
                        """id|input|int|intern|isinstance|issubclass|iter|len|list|locals|long|map|max|memoryview|""" +
                        """min|next|object|oct|open|ord|pow|property|range|raw_input|reduce|reload|repr|reversed|""" +
                        """round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|unichr|unicode|""" +
                        """vars|xrange|zip)\b"""
                )
            )
        ),
        token("boolean", pattern(compile("""\b(?:True|False|None)\b"""))),
        token(
            "number",
            pattern(
                compile(
                    """(?:\b(?=\d)|\B(?=\.))(?:0[bo])?(?:(?:\d|0x[\da-f])[\da-f]*\.?\d*|\.\d+)(?:e[+-]?\d+)?j?\b""",
                    CASE_INSENSITIVE
                )
            )
        ),
        token(
            "operator",
            pattern(compile("""[-+%=]=?|!=|\*\*?=?|//?=?|<[<=>]?|>[=>]?|[&|^~]|\b(?:or|and|not)\b"""))
        ),
        token("punctuation", pattern(compile("""[{}\[\];(),.:]""")))
    )

    private fun createScala(prism4j: Prism4j): Prism4j.Grammar {
        val scala = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "java"),
            "scala",
            { it.name() != "class-name" && it.name() != "function" },
            token(
                "keyword",
                pattern(
                    compile(
                        """<-|=>|\b(?:abstract|case|catch|class|def|do|else|extends|final|finally|for|""" +
                            """forSome|if|implicit|import|lazy|match|new|null|object|override|package|private|""" +
                            """protected|return|sealed|self|super|this|throw|trait|try|type|val|var|while|with|""" +
                            """yield)\b"""
                    )
                )
            ),
            token(
                "string",
                pattern(compile("\"\"\"[\\s\\S]*?\"\"\""), false, true),
                pattern(compile("""(["'])(?:\\.|(?!\1)[^\\\r\n])*\1"""), false, true)
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b0x[\da-f]*\.?[\da-f]+|(?:\b\d+\.?\d*|\B\.\d+)(?:e\d+)?[dfl]?""",
                        CASE_INSENSITIVE
                    )
                )
            )
        )

        scala.tokens().add(
            token("symbol", pattern(compile("""'[^\d\s\\]\w*""")))
        )

        GrammarUtils.insertBeforeToken(
            scala,
            "number",
            token(
                "builtin",
                pattern(
                    compile(
                        """\b(?:String|Int|Long|Short|Byte|Boolean|Double|Float|Char|Any|AnyRef|AnyVal|Unit|""" +
                            """Nothing)\b"""
                    )
                )
            )
        )

        return scala
    }

    private fun createSql(): Prism4j.Grammar = grammar(
        "sql",
        token(
            "comment",
            pattern(
                compile("""(^|[^\\])(?:/\*[\s\S]*?\*/|(?:--|//|#).*)"""),
                true
            )
        ),
        token(
            "string",
            pattern(
                compile("""(^|[^@\\])(["'])(?:\\[\s\S]|(?!\2)[^\\])*\2"""),
                true,
                true
            )
        ),
        token(
            "variable",
            pattern(compile("""@[\w.$]+|@(["'`])(?:\\[\s\S]|(?!\1)[^\\])+\1"""))
        ),
        token(
            "function",
            pattern(
                compile(
                    """\b(?:AVG|COUNT|FIRST|FORMAT|LAST|LCASE|LEN|MAX|MID|MIN|MOD|NOW|ROUND|SUM|UCASE)(?=\s*\()""",
                    CASE_INSENSITIVE
                )
            )
        ),
        token(
            "keyword",
            pattern(
                compile(
                    """\b(?:ACTION|ADD|AFTER|ALGORITHM|ALL|ALTER|ANALYZE|ANY|APPLY|AS|ASC|""" +
                        """AUTHORIZATION|AUTO_INCREMENT|BACKUP|BDB|BEGIN|BERKELEYDB|BIGINT|BINARY|BIT|BLOB|""" +
                        """BOOL|BOOLEAN|BREAK|BROWSE|BTREE|BULK|BY|CALL|CASCADED?|CASE|CHAIN|CHAR(?:ACTER|""" +
                        """SET)?|CHECK(?:POINT)?|CLOSE|CLUSTERED|COALESCE|COLLATE|COLUMNS?|COMMENT|""" +
                        """COMMIT(?:TED)?|COMPUTE|CONNECT|CONSISTENT|CONSTRAINT|CONTAINS(?:TABLE)?|""" +
                        """CONTINUE|CONVERT|CREATE|CROSS|CURRENT(?:_DATE|_TIME|_TIMESTAMP|_USER)?|CURSOR|""" +
                        """CYCLE|DATA(?:BASES?)?|DATE(?:TIME)?|DAY|DBCC|DEALLOCATE|DEC|DECIMAL|DECLARE|""" +
                        """DEFAULT|DEFINER|DELAYED|DELETE|DELIMITERS?|DENY|DESC|DESCRIBE|DETERMINISTIC|""" +
                        """DISABLE|DISCARD|DISK|DISTINCT|DISTINCTROW|DISTRIBUTED|DO|DOUBLE|DROP|DUMMY|""" +
                        """DUMP(?:FILE)?|DUPLICATE|ELSE(?:IF)?|ENABLE|ENCLOSED|END|ENGINE|ENUM|ERRLVL|""" +
                        """ERRORS|ESCAPED?|EXCEPT|EXEC(?:UTE)?|EXISTS|EXIT|EXPLAIN|EXTENDED|FETCH|FIELDS|""" +
                        """FILE|FILLFACTOR|FIRST|FIXED|FLOAT|FOLLOWING|FOR(?: EACH ROW)?|FORCE|FOREIGN|""" +
                        """FREETEXT(?:TABLE)?|FROM|FULL|FUNCTION|GEOMETRY(?:COLLECTION)?|GLOBAL|GOTO|GRANT|""" +
                        """GROUP|HANDLER|HASH|HAVING|HOLDLOCK|HOUR|IDENTITY(?:_INSERT|COL)?|IF|IGNORE|""" +
                        """IMPORT|INDEX|INFILE|INNER|INNODB|INOUT|INSERT|INT|INTEGER|INTERSECT|INTERVAL|""" +
                        """INTO|INVOKER|ISOLATION|ITERATE|JOIN|KEYS?|KILL|LANGUAGE|LAST|LEAVE|LEFT|LEVEL|""" +
                        """LIMIT|LINENO|LINES|LINESTRING|LOAD|LOCAL|LOCK|LONG(?:BLOB|TEXT)|LOOP|""" +
                        """MATCH(?:ED)?|MEDIUM(?:BLOB|INT|TEXT)|MERGE|MIDDLEINT|MINUTE|MODE|MODIFIES|""" +
                        """MODIFY|MONTH|MULTI(?:LINESTRING|POINT|POLYGON)|NATIONAL|NATURAL|NCHAR|NEXT|NO|""" +
                        """NONCLUSTERED|NULLIF|NUMERIC|OFF?|OFFSETS?|ON|OPEN(?:DATASOURCE|QUERY|ROWSET)?|""" +
                        """OPTIMIZE|OPTION(?:ALLY)?|ORDER|OUT(?:ER|FILE)?|OVER|PARTIAL|PARTITION|PERCENT|""" +
                        """PIVOT|PLAN|POINT|POLYGON|PRECEDING|PRECISION|PREPARE|PREV|PRIMARY|PRINT|""" +
                        """PRIVILEGES|PROC(?:EDURE)?|PUBLIC|PURGE|QUICK|RAISERROR|READS?|REAL|RECONFIGURE|""" +
                        """REFERENCES|RELEASE|RENAME|REPEAT(?:ABLE)?|REPLACE|REPLICATION|REQUIRE|RESIGNAL|""" +
                        """RESTORE|RESTRICT|RETURNS?|REVOKE|RIGHT|ROLLBACK|ROUTINE|ROW(?:COUNT|GUIDCOL|S)?|""" +
                        """RTREE|RULE|SAVE(?:POINT)?|SCHEMA|SECOND|SELECT|SERIAL(?:IZABLE)?|""" +
                        """SESSION(?:_USER)?|SET(?:USER)?|SHARE|SHOW|SHUTDOWN|SIMPLE|SMALLINT|SNAPSHOT|""" +
                        """SOME|SONAME|SQL|START(?:ING)?|STATISTICS|STATUS|STRIPED|SYSTEM_USER|TABLES?|""" +
                        """TABLESPACE|TEMP(?:ORARY|TABLE)?|TERMINATED|TEXT(?:SIZE)?|THEN|TIME(?:STAMP)?|""" +
                        """TINY(?:BLOB|INT|TEXT)|TOP?|TRAN(?:SACTIONS?)?|TRIGGER|TRUNCATE|TSEQUAL|TYPES?|""" +
                        """UNBOUNDED|UNCOMMITTED|UNDEFINED|UNION|UNIQUE|UNLOCK|UNPIVOT|UNSIGNED|""" +
                        """UPDATE(?:TEXT)?|USAGE|USE|USER|USING|VALUES?|VAR(?:BINARY|CHAR|CHARACTER|YING)|""" +
                        """VIEW|WAITFOR|WARNINGS|WHEN|WHERE|WHILE|WITH(?: ROLLUP|IN)?|WORK|WRITE(?:TEXT)?|""" +
                        """YEAR)\b""",
                    CASE_INSENSITIVE
                )
            )
        ),
        token(
            "boolean",
            pattern(
                compile("""\b(?:TRUE|FALSE|NULL)\b""", CASE_INSENSITIVE)
            )
        ),
        token(
            "number",
            pattern(
                compile("""\b0x[\da-f]+\b|\b\d+\.?\d*|\B\.\d+\b""", CASE_INSENSITIVE)
            )
        ),
        token(
            "operator",
            pattern(
                compile(
                    """[-+*/=%^~]|&&?|\|\|?|!=?|<(?:=>?|<|>)?|>[>=]?|\b(?:AND|BETWEEN|IN|LIKE|NOT|OR|""" +
                        """IS|DIV|REGEXP|RLIKE|SOUNDS LIKE|XOR)\b""",
                    CASE_INSENSITIVE
                )
            )
        ),
        token("punctuation", pattern(compile("""[;\[\]()`,.]""")))
    )

    private fun createSwift(prism4j: Prism4j): Prism4j.Grammar {
        val interpolationInside = grammar(
            "inside",
            token("delimiter", pattern(compile("""^\\\(|\)$"""), false, false, "variable"))
        )

        val swift = GrammarUtils.extend(
            GrammarUtils.require(prism4j, "clike"),
            "swift",
            token(
                "string",
                pattern(
                    compile("""(["'])(\\(?:\((?:[^()]|\([^)]+\))+\)|\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1"""),
                    false,
                    true,
                    null,
                    grammar(
                        "inside",
                        token(
                            "interpolation",
                            pattern(
                                compile("""\\\((?:[^()]|\([^)]+\))+\)"""),
                                false,
                                false,
                                null,
                                interpolationInside
                            )
                        )
                    )
                )
            ),
            token(
                "keyword",
                pattern(
                    compile(
                        """\b(?:as|associativity|break|case|catch|class|continue|convenience|default|defer|""" +
                            """deinit|didSet|do|dynamic(?:Type)?|else|enum|extension|fallthrough|final|for|""" +
                            """func|get|guard|if|import|in|infix|init|inout|internal|is|lazy|left|let|mutating|""" +
                            """new|none|nonmutating|operator|optional|override|postfix|precedence|prefix|""" +
                            """private|protocol|public|repeat|required|rethrows|return|right|safe|self|Self|""" +
                            """set|static|struct|subscript|super|switch|throws?|try|Type|typealias|unowned|""" +
                            """unsafe|var|weak|where|while|willSet|__(?:COLUMN__|FILE__|FUNCTION__|LINE__))\b"""
                    )
                )
            ),
            token(
                "number",
                pattern(
                    compile(
                        """\b(?:[\d_]+(?:\.[\de_]+)?|0x[a-f0-9_]+(?:\.[a-f0-9p_]+)?|0b[01_]+|0o[0-7_]+)\b""",
                        CASE_INSENSITIVE
                    )
                )
            )
        )

        swift.tokens().addAll(
            listOf(
                token(
                    "constant",
                    pattern(compile("""\b(?:nil|[A-Z_]{2,}|k[A-Z][A-Za-z_]+)\b"""))
                ),
                token(
                    "atrule",
                    pattern(
                        compile(
                            """@\b(?:IB(?:Outlet|Designable|Action|Inspectable)|class_protocol|exported|""" +
                                """noreturn|NS(?:Copying|Managed)|objc|UIApplicationMain|auto_closure)\b"""
                        )
                    )
                ),
                token(
                    "builtin",
                    pattern(
                        compile(
                            """\b(?:[A-Z]\S+|abs|advance|alignof(?:Value)?|assert|contains|count(?:Elements)?|""" +
                                """debugPrint(?:ln)?|distance|drop(?:First|Last)|dump|enumerate|equal|filter|find|""" +
                                """first|getVaList|indices|isEmpty|join|last|lexicographicalCompare|map|""" +
                                """max(?:Element)?|min(?:Element)?|numericCast|overlaps|partition|print(?:ln)?|""" +
                                """reduce|reflect|reverse|sizeof(?:Value)?|sort(?:ed)?|split|startsWith|""" +
                                """stride(?:of(?:Value)?)?|suffix|swap|toDebugString|toString|transcode|""" +
                                """underestimateCount|unsafeBitCast|with(?:ExtendedLifetime|""" +
                                """Unsafe(?:MutablePointers?|Pointers?)|VaList))\b"""
                        )
                    )
                )
            )
        )

        interpolationInside.tokens().addAll(swift.tokens())

        return swift
    }

    private fun createYaml(): Prism4j.Grammar = grammar(
        "yaml",
        token(
            "scalar",
            pattern(
                compile("""([\-:]\s*(?:!\S+)?[ \t]*[|>])[ \t]*((?:\r?\n|\r)[ \t]+)[^\r\n]+(?:\2[^\r\n]+)*"""),
                true,
                false,
                "string"
            )
        ),
        token("comment", pattern(compile("#.*"))),
        token(
            "key",
            pattern(
                compile("""(\s*(?:^|[:\-,\[{\r\n?])[ \t]*(?:!\S+)?[ \t]*)[^\r\n{\[\]},#\s]+?(?=\s*:\s)"""),
                true,
                false,
                "atrule"
            )
        ),
        token(
            "directive",
            pattern(
                compile("""(^[ \t]*)%.+""", MULTILINE),
                true,
                false,
                "important"
            )
        ),
        token(
            "datetime",
            pattern(
                compile(
                    """([:\-,\[{]\s*(?:!\S+)?[ \t]*)(?:\d{4}-\d\d?-\d\d?(?:[tT]|""" +
                        """[ \t]+)\d\d?:\d{2}:\d{2}(?:\.\d*)?[ \t]*(?:Z|[-+]\d\d?(?::\d{2})?)?|""" +
                        """\d{4}-\d{2}-\d{2}|\d\d?:\d{2}(?::\d{2}(?:\.\d*)?)?)(?=[ \t]*(?:$|,|]|\}))""",
                    MULTILINE
                ),
                true,
                false,
                "number"
            )
        ),
        token(
            "boolean",
            pattern(
                compile(
                    """([:\-,\[{]\s*(?:!\S+)?[ \t]*)(?:true|false)[ \t]*(?=$|,|]|\})""",
                    MULTILINE or CASE_INSENSITIVE
                ),
                true,
                false,
                "important"
            )
        ),
        token(
            "null",
            pattern(
                compile(
                    """([:\-,\[{]\s*(?:!\S+)?[ \t]*)(?:null|~)[ \t]*(?=$|,|]|\})""",
                    MULTILINE or CASE_INSENSITIVE
                ),
                true,
                false,
                "important"
            )
        ),
        token(
            "string",
            pattern(
                compile(
                    """([:\-,\[{]\s*(?:!\S+)?[ \t]*)(["'])(?:(?!\2)[^\\\r\n]|\\.)*\2(?=[ \t]*(?:$|,|]|\}))""",
                    MULTILINE
                ),
                true,
                true
            )
        ),
        token(
            "number",
            pattern(
                compile(
                    """([:\-,\[{]\s*(?:!\S+)?[ \t]*)[+-]?(?:0x[\da-f]+|0o[0-7]+|(?:\d+\.?\d*|""" +
                        """\.?\d+)(?:e[+-]?\d+)?|\.inf|\.nan)[ \t]*(?=$|,|]|\})""",
                    MULTILINE or CASE_INSENSITIVE
                ),
                true
            )
        ),
        token("tag", pattern(compile("""!\S+"""))),
        token("important", pattern(compile("""[&*]\w+"""))),
        token("punctuation", pattern(compile("""---|[:\[\]{}\-,|>?]|\.\.\.""")))
    )

    companion object {
        private const val MARKUP = "markup"
        private const val CSS = "css"
        private const val JAVASCRIPT = "javascript"

        private val ALIASES = mapOf(
            "dotnet" to "csharp",
            "js" to JAVASCRIPT,
            "jsonp" to "json",
            "xml" to MARKUP,
            "html" to MARKUP,
            "mathml" to MARKUP,
            "svg" to MARKUP
        )
    }
}
