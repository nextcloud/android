/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package third_parties.aosp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

/**
 * SQL Tokenizer specialized to extract tokens from SQL (snippets).
 *
 * Originally from AOSP: https://github.com/aosp-mirror/platform_frameworks_base/blob/0e66ea6f3221aa8ccbb78ce38fbcaa67d8ea94f9/core/java/android/database/sqlite/SQLiteQueryBuilder.java
 * Backported to be usable with AndroidX under api 24.
 */
public class SQLiteTokenizer {
    private static boolean isAlpha(char ch) {
        return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || (ch == '_');
    }

    private static boolean isNum(char ch) {
        return ('0' <= ch && ch <= '9');
    }

    private static boolean isAlNum(char ch) {
        return isAlpha(ch) || isNum(ch);
    }

    private static boolean isAnyOf(char ch, String set) {
        return set.indexOf(ch) >= 0;
    }

    private static IllegalArgumentException genException(String message, String sql) {
        throw new IllegalArgumentException(message + " in '" + sql + "'");
    }

    private static char peek(String s, int index) {
        return index < s.length() ? s.charAt(index) : '\0';
    }

    public static final int OPTION_NONE = 0;

    /**
     * Require that SQL contains only tokens; any comments or values will result
     * in an exception.
     */
    public static final int OPTION_TOKEN_ONLY = 1 << 0;

    /**
     * Tokenize the given SQL, returning the list of each encountered token.
     *
     * @throws IllegalArgumentException if invalid SQL is encountered.
     */
    public static List<String> tokenize(@Nullable String sql, int options) {
        final ArrayList<String> res = new ArrayList<>();
        tokenize(sql, options, res::add);
        return res;
    }

    /**
     * Tokenize the given SQL, sending each encountered token to the given
     * {@link Consumer}.
     *
     * @throws IllegalArgumentException if invalid SQL is encountered.
     */
    public static void tokenize(@Nullable String sql, int options, Consumer<String> checker) {
        if (sql == null) {
            return;
        }
        int pos = 0;
        final int len = sql.length();
        while (pos < len) {
            final char ch = peek(sql, pos);

            // Regular token.
            if (isAlpha(ch)) {
                final int start = pos;
                do {
                    pos++;
                } while (isAlNum(peek(sql, pos)));
                final int end = pos;

                final String token = sql.substring(start, end);
                checker.accept(token);

                continue;
            }

            // Handle quoted tokens
            if (isAnyOf(ch, "'\"`")) {
                final int quoteStart = pos;
                pos++;

                for (;;) {
                    pos = sql.indexOf(ch, pos);
                    if (pos < 0) {
                        throw genException("Unterminated quote", sql);
                    }
                    if (peek(sql, pos + 1) != ch) {
                        break;
                    }
                    // Quoted quote char -- e.g. "abc""def" is a single string.
                    pos += 2;
                }
                final int quoteEnd = pos;
                pos++;

                if (ch != '\'') {
                    // Extract the token
                    final String tokenUnquoted = sql.substring(quoteStart + 1, quoteEnd);

                    final String token;

                    // Unquote if needed. i.e. "aa""bb" -> aa"bb
                    if (tokenUnquoted.indexOf(ch) >= 0) {
                        token = tokenUnquoted.replaceAll(
                            String.valueOf(ch) + ch, String.valueOf(ch));
                    } else {
                        token = tokenUnquoted;
                    }
                    checker.accept(token);
                } else {
                    if ((options &= OPTION_TOKEN_ONLY) != 0) {
                        throw genException("Non-token detected", sql);
                    }
                }
                continue;
            }
            // Handle tokens enclosed in [...]
            if (ch == '[') {
                final int quoteStart = pos;
                pos++;

                pos = sql.indexOf(']', pos);
                if (pos < 0) {
                    throw genException("Unterminated quote", sql);
                }
                final int quoteEnd = pos;
                pos++;

                final String token = sql.substring(quoteStart + 1, quoteEnd);

                checker.accept(token);
                continue;
            }
            if ((options &= OPTION_TOKEN_ONLY) != 0) {
                throw genException("Non-token detected", sql);
            }

            // Detect comments.
            if (ch == '-' && peek(sql, pos + 1) == '-') {
                pos += 2;
                pos = sql.indexOf('\n', pos);
                if (pos < 0) {
                    // We disallow strings ending in an inline comment.
                    throw genException("Unterminated comment", sql);
                }
                pos++;

                continue;
            }
            if (ch == '/' && peek(sql, pos + 1) == '*') {
                pos += 2;
                pos = sql.indexOf("*/", pos);
                if (pos < 0) {
                    throw genException("Unterminated comment", sql);
                }
                pos += 2;

                continue;
            }

            // Semicolon is never allowed.
            if (ch == ';') {
                throw genException("Semicolon is not allowed", sql);
            }

            // For this purpose, we can simply ignore other characters.
            // (Note it doesn't handle the X'' literal properly and reports this X as a token,
            // but that should be fine...)
            pos++;
        }
    }

    /**
     * Test if given token is a
     * <a href="https://www.sqlite.org/lang_keywords.html">SQLite reserved
     * keyword</a>.
     */
    public static boolean isKeyword(@NonNull String token) {
        return switch (token.toUpperCase(Locale.US)) {
            case "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ATTACH",
                 "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BINARY", "BY", "CASCADE", "CASE", "CAST", "CHECK",
                 "COLLATE", "COLUMN", "COMMIT", "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE",
                 "CURRENT_TIME", "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC",
                 "DETACH", "DISTINCT", "DO", "DROP", "EACH", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUDE", "EXCLUSIVE",
                 "EXISTS", "EXPLAIN", "FAIL", "FILTER", "FOLLOWING", "FOR", "FOREIGN", "FROM", "FULL", "GLOB", "GROUP",
                 "GROUPS", "HAVING", "IF", "IGNORE", "IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER",
                 "INSERT", "INSTEAD", "INTERSECT", "INTO", "IS", "ISNULL", "JOIN", "KEY", "LEFT", "LIKE", "LIMIT",
                 "MATCH", "NATURAL", "NO", "NOCASE", "NOT", "NOTHING", "NOTNULL", "NULL", "OF", "OFFSET", "ON", "OR",
                 "ORDER", "OTHERS", "OUTER", "OVER", "PARTITION", "PLAN", "PRAGMA", "PRECEDING", "PRIMARY", "QUERY",
                 "RAISE", "RANGE", "RECURSIVE", "REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME", "REPLACE",
                 "RESTRICT", "RIGHT", "ROLLBACK", "ROW", "ROWS", "RTRIM", "SAVEPOINT", "SELECT", "SET", "TABLE", "TEMP",
                 "TEMPORARY", "THEN", "TIES", "TO", "TRANSACTION", "TRIGGER", "UNBOUNDED", "UNION", "UNIQUE", "UPDATE",
                 "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL", "WHEN", "WHERE", "WINDOW", "WITH", "WITHOUT" -> true;
            default -> false;
        };
    }

    /**
     * Test if given token is a
     * <a href="https://www.sqlite.org/lang_corefunc.html">SQLite reserved
     * function</a>.
     */
    public static boolean isFunction(@NonNull String token) {
        return switch (token.toLowerCase(Locale.US)) {
            case "abs", "avg", "char", "coalesce", "count", "glob", "group_concat", "hex", "ifnull", "instr", "length",
                 "like", "likelihood", "likely", "lower", "ltrim", "max", "min", "nullif", "random", "randomblob",
                 "replace", "round", "rtrim", "substr", "sum", "total", "trim", "typeof", "unicode", "unlikely",
                 "upper", "zeroblob" -> true;
            default -> false;
        };
    }

    /**
     * Test if given token is a
     * <a href="https://www.sqlite.org/datatype3.html">SQLite reserved type</a>.
     */
    public static boolean isType(@NonNull String token) {
        return switch (token.toUpperCase(Locale.US)) {
            case "INT", "INTEGER", "TINYINT", "SMALLINT", "MEDIUMINT", "BIGINT", "INT2", "INT8", "CHARACTER", "VARCHAR",
                 "NCHAR", "NVARCHAR", "TEXT", "CLOB", "BLOB", "REAL", "DOUBLE", "FLOAT", "NUMERIC", "DECIMAL",
                 "BOOLEAN", "DATE", "DATETIME" -> true;
            default -> false;
        };
    }
}
