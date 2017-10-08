package lexer;

import lexer.Token.Kind;
import util.Todo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static control.Control.ConLexer.dump;

public class Lexer {
    private static final Map<String, Kind> keywords = new HashMap<>();

    static {
        keywords.put("boolean", Kind.TOKEN_BOOLEAN);
        keywords.put("class", Kind.TOKEN_CLASS);
        keywords.put("extends", Kind.TOKEN_EXTENDS);
        keywords.put("false", Kind.TOKEN_FALSE);
        keywords.put("if", Kind.TOKEN_IF);
        keywords.put("int", Kind.TOKEN_INT);
        keywords.put("length", Kind.TOKEN_LENGTH);
        keywords.put("main", Kind.TOKEN_MAIN);
        keywords.put("new", Kind.TOKEN_NEW);
        keywords.put("out", Kind.TOKEN_OUT);
        keywords.put("println", Kind.TOKEN_PRINTLN);
        keywords.put("public", Kind.TOKEN_PUBLIC);
        keywords.put("return", Kind.TOKEN_RETURN);
        keywords.put("static", Kind.TOKEN_STATIC);
        keywords.put("String", Kind.TOKEN_STRING);
        keywords.put("System", Kind.TOKEN_SYSTEM);
        keywords.put("this", Kind.TOKEN_THIS);
        keywords.put("true", Kind.TOKEN_TRUE);
        keywords.put("void",  Kind.TOKEN_VOID);
        keywords.put("while", Kind.TOKEN_WHILE);
    }



    private String fname; // the input file name to be compiled
    private InputStream fstream; // input stream for the above file
    private Integer linePos = 1;
    private Integer columnPos = 1;


    public Lexer(String fname, InputStream fstream) {
        this.fname = fname;
        this.fstream = fstream;
    }

    // When called, return the next token (refer to the code "Token.java")
    // from the input stream.
    // Return TOKEN_EOF when reaching the end of the input stream.
    private Token nextTokenInternal() throws Exception {
        int c = this.fstream.read();
        if (-1 == c)
            // The value for "lineNum" is now "null",
            // you should modify this to an appropriate
            // line number for the "EOF" token.
            return new Token(Kind.TOKEN_EOF, linePos, columnPos);

        // skip all kinds of "blanks"
        c = skipBlanks(c);
        if (-1 == c)
            return new Token(Kind.TOKEN_EOF, linePos, columnPos);

        if (isAlpha(c)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append((char) c);
                columnPos++;
                this.fstream.mark(1);
                c = this.fstream.read();
            } while (isAlpha(c) || isDigit(c));

            this.fstream.reset();


            String lexeme = sb.toString();

            if (keywords.containsKey(lexeme)) {
                return new Token(keywords.get(lexeme), linePos, columnPos);
            } else {
                return new Token(Kind.TOKEN_ID, linePos, columnPos, lexeme);
            }
        }

        if (isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append((char) c);
                columnPos++;
                this.fstream.mark(1);
                c = this.fstream.read();
            } while (isDigit(c));

            this.fstream.reset();

            return new Token(Kind.TOKEN_NUM, linePos, columnPos, sb.toString());
        }

        switch (c) {
            case '+':
                return new Token(Kind.TOKEN_ADD, linePos, ++columnPos);
            case '-':
                return new Token(Kind.TOKEN_SUB, linePos, ++columnPos);
            case '*':
                return new Token(Kind.TOKEN_TIMES, linePos, ++columnPos);
            case '=':
                return new Token(Kind.TOKEN_ASSIGN, linePos, ++columnPos);
            case ',':
                return new Token(Kind.TOKEN_COMMER, linePos, ++columnPos);
            case '.':
                return new Token(Kind.TOKEN_DOT, linePos, ++columnPos);
            case '{':
                return new Token(Kind.TOKEN_LBRACE, linePos, ++columnPos);
            case '[':
                return new Token(Kind.TOKEN_LBRACK, linePos, ++columnPos);
            case '(':
                return new Token(Kind.TOKEN_LPAREN, linePos, ++columnPos);
            case '<':
                return new Token(Kind.TOKEN_LT, linePos, ++columnPos);
            case '!':
                return new Token(Kind.TOKEN_NOT, linePos, ++columnPos);
            case '}':
                return new Token(Kind.TOKEN_RBRACE, linePos, ++columnPos);
            case ']':
                return new Token(Kind.TOKEN_RBRACK, linePos, ++columnPos);
            case ')':
                return new Token(Kind.TOKEN_RPAREN, linePos, ++columnPos);
            case ';':
                return new Token(Kind.TOKEN_SEMI, linePos, ++columnPos);
            case '&':
                if (this.fstream.read() == '&') {
                    return new Token(Kind.TOKEN_AND, linePos, columnPos+=2);
                }
            case '/':
                int cc = this.fstream.read();
                if ('/' == cc) {

                    StringBuilder comments = new StringBuilder();

                    do {
                        cc = this.fstream.read();
                        comments.append((char)cc);
                    } while (cc != '\n' && cc != -1);

                    if (cc == '\n') {
                        linePos++;
                        columnPos = 1;
                    }

                    System.out.println(comments);

                    return nextTokenInternal();
                } else if ('*' == cc) {

                    StringBuilder comments = new StringBuilder();

                    int pre;
                    do {
                        pre = cc;
                        cc = this.fstream.read();
                        if (cc == '\n') {
                            linePos++;
                            columnPos = 1;
                        }
                        if (pre == '*' && cc == '/') {
                            comments.delete(comments.length() - 1, comments.length());
                            break;
                        }
                        comments.append((char)cc);
                    } while (true);

                    System.out.println(comments);
                    return nextTokenInternal();
                }
            default:

                // Lab 1, exercise 2: supply missing code to
                // lex other kinds of tokens.
                // Hint: think carefully about the basic
                // data structure and algorithms. The code
                // is not that much and may be less than 50 lines. If you
                // find you are writing a lot of code, you
                // are on the wrong way.
                new Todo();
                return null;
        }
    }

    private boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private int skipBlanks(int c) throws IOException {
        while (true) {
            if (' ' == c) {
                columnPos++;
            } else if ('\r' == c) {
                columnPos = 1;
            } else if ('\n' == c) {
                linePos++;
                columnPos = 1;
            } else if ('\t' == c) {
                columnPos+=4;
            } else {
                break;
            }

            c = this.fstream.read();

        }
        return c;
    }

    public Token nextToken() {
        Token t = null;

        try {
            t = this.nextTokenInternal();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (dump)
            System.out.println(t);
        return t;
    }
}
