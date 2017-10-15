package parser;

import ast.Ast;
import ast.BaseAcceptable;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

import java.util.LinkedList;

public class Parser {
    Lexer lexer;
    Token current;

    public Parser(String fname, java.io.InputStream fstream) {
        lexer = new Lexer(fname, fstream);
        current = lexer.nextToken();
    }

    // /////////////////////////////////////////////
    // utility methods to connect the lexer
    // and the parser.

    private void advance() {
        current = lexer.nextToken();
    }

    private void eatToken(Kind kind) {
        if (kind == current.kind)
            advance();
        else {
            System.out.println("Expects: " + kind.toString());
            System.out.println("But got: " + current.kind.toString());
            System.exit(1);
        }
    }

    private void error() {
        System.out.println("Syntax error: compilation aborting...\n");
        System.exit(1);
        return;
    }

    // ////////////////////////////////////////////////////////////
    // below are method for parsing.

    // A bunch of parsing methods to parse expressions. The messy
    // parts are to deal with precedence and associativity.

    // ExpList -> Exp ExpRest*
    // ->
    // ExpRest -> , Exp
    private LinkedList<Ast.Exp.T> parseExpList() {
        LinkedList<Ast.Exp.T> expList = new LinkedList<>();
        if (current.kind == Kind.TOKEN_RPAREN)
            return expList;
        expList.add(parseExp());
        while (current.kind == Kind.TOKEN_COMMER) {
            advance();
            expList.add(parseExp());
        }
        return expList;
    }

    // AtomExp -> (exp)
    // -> INTEGER_LITERAL
    // -> true
    // -> false
    // -> this
    // -> id
    // -> new int [exp]
    // -> new id ()
    private Ast.Exp.T parseAtomExp() {
        switch (current.kind) {
            case TOKEN_LPAREN:
                advance();
                Ast.Exp.T parenExp = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                return parenExp;
            case TOKEN_NUM:
                String lexeme = current.lexeme;
                advance();
                return setLineNumber(new Ast.Exp.Num(Integer.parseInt(lexeme)));
            case TOKEN_TRUE:
                advance();
                return setLineNumber(new Ast.Exp.True());
            case TOKEN_FALSE:
                advance();
                return setLineNumber(new Ast.Exp.False());
            case TOKEN_THIS:
                advance();
                return setLineNumber(new Ast.Exp.This());
            case TOKEN_ID:
                String id = current.lexeme;
                advance();
                return setLineNumber(new Ast.Exp.Id(id));
            case TOKEN_NEW:
                advance();
                if (current.kind == Token.Kind.TOKEN_INT) {
                    advance();
                    eatToken(Kind.TOKEN_LBRACK);
                    Ast.Exp.T arrayLength = parseExp();
                    eatToken(Kind.TOKEN_RBRACK);
                    return setLineNumber(new Ast.Exp.NewIntArray(arrayLength));
                } else if (current.kind == Token.Kind.TOKEN_ID) {
                    String idName = current.lexeme;
                    advance();
                    eatToken(Kind.TOKEN_LPAREN);
                    eatToken(Kind.TOKEN_RPAREN);
                    return setLineNumber(new Ast.Exp.NewObject(idName));
                }
                error();
                return null;

            default:
                error();
                return null;
        }
    }

    // NotExp -> AtomExp
    // -> AtomExp .id (expList)
    // -> AtomExp [exp]
    // -> AtomExp .length
    private Ast.Exp.T parseNotExp() {
        Ast.Exp.T atomExp = parseAtomExp();
        while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
            if (current.kind == Kind.TOKEN_DOT) {
                advance();
                if (current.kind == Kind.TOKEN_LENGTH) {
                    advance();
                    return setLineNumber(new Ast.Exp.Length(atomExp));
                }
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                eatToken(Kind.TOKEN_LPAREN);
                LinkedList<Ast.Exp.T> arguments = parseExpList();
                eatToken(Kind.TOKEN_RPAREN);

                atomExp = setLineNumber(new Ast.Exp.Call(atomExp, id, arguments));
            } else {
                advance();
                Ast.Exp.T index = parseExp();
                eatToken(Kind.TOKEN_RBRACK);
                atomExp = setLineNumber(new Ast.Exp.ArraySelect(atomExp, index));
            }
        }
        return atomExp;
    }

    // TimesExp -> ! TimesExp
    // -> NotExp
    private Ast.Exp.T parseTimesExp() {
        int notTimes = 0;
        while (current.kind == Kind.TOKEN_NOT) {
            advance();
            notTimes++;
        }
        Ast.Exp.T exp = parseNotExp();
        for (int i = 0; i < notTimes; i++) {
            exp = setLineNumber(new Ast.Exp.Not(exp));
        }
        return exp;
    }

    // AddSubExp -> TimesExp * TimesExp
    // -> TimesExp
    private Ast.Exp.T parseAddSubExp() {
        Ast.Exp.T exp = parseTimesExp();
        while (current.kind == Kind.TOKEN_TIMES) {
            advance();
            exp = setLineNumber(new Ast.Exp.Times(exp, parseTimesExp()));
        }
        return exp;
    }

    // LtExp -> AddSubExp + AddSubExp
    // -> AddSubExp - AddSubExp
    // -> AddSubExp
    private Ast.Exp.T parseLtExp() {
        Ast.Exp.T exp = parseAddSubExp();
        while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
            boolean isAdd = current.kind == Kind.TOKEN_ADD;
            advance();
            Ast.Exp.T right = parseAddSubExp();
            if (isAdd) {
                exp = setLineNumber(new Ast.Exp.Add(exp, right));
            } else {
                exp = setLineNumber(new Ast.Exp.Sub(exp, right));
            }
        }
        return exp;
    }

    // AndExp -> LtExp < LtExp
    // -> LtExp
    private Ast.Exp.T parseAndExp() {
        Ast.Exp.T exp = parseLtExp();
        while (current.kind == Kind.TOKEN_LT) {
            advance();
            exp = setLineNumber(new Ast.Exp.Lt(exp, parseLtExp()));
        }
        return exp;
    }

    // Exp -> AndExp && AndExp
    // -> AndExp
    private Ast.Exp.T parseExp() {
        Ast.Exp.T exp = parseAndExp();
        while (current.kind == Kind.TOKEN_AND) {
            advance();
            exp = setLineNumber(new Ast.Exp.And(exp, parseAndExp()));
        }
        return exp;
    }

    // Statement -> { Statement* }
    // -> if ( Exp ) Statement else Statement
    // -> while ( Exp ) Statement
    // -> System.out.println ( Exp ) ;
    // -> id = Exp ;
    // -> id [ Exp ]= Exp ;
    private Ast.Stm.T parseStatement() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a statement.
        switch (current.kind) {
            case TOKEN_LBRACE:
                advance();
                LinkedList<Ast.Stm.T> stmts = parseStatements();
                eatToken(Kind.TOKEN_RBRACE);
                return setLineNumber(new Ast.Stm.Block(stmts));
            case TOKEN_IF:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T ifCondition = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                Ast.Stm.T then = parseStatement();
                eatToken(Kind.TOKEN_ELSE);
                Ast.Stm.T elsz = parseStatement();
                return setLineNumber(new Ast.Stm.If(ifCondition, then, elsz));
            case TOKEN_WHILE:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T condition = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                Ast.Stm.T stmt = parseStatement();
                return setLineNumber(new Ast.Stm.While(condition, stmt));
            case TOKEN_SYSTEM:
                advance();
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_OUT);
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_PRINTLN);
                eatToken(Kind.TOKEN_LPAREN);
                Ast.Exp.T printExp = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                eatToken(Kind.TOKEN_SEMI);
                return setLineNumber(new Ast.Stm.Print(printExp));
            case TOKEN_ID:

                String id = current.lexeme;
                advance();
                if (current.kind == Token.Kind.TOKEN_ASSIGN) {
                    advance();
                    Ast.Exp.T assignExp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    return setLineNumber(new Ast.Stm.Assign(id, assignExp));
                } else if (current.kind == Token.Kind.TOKEN_LBRACK) {
                    advance();
                    Ast.Exp.T index = parseExp();
                    eatToken(Kind.TOKEN_RBRACK);
                    eatToken(Kind.TOKEN_ASSIGN);
                    Ast.Exp.T assignArrayExp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    return setLineNumber(new Ast.Stm.AssignArray(id, index, assignArrayExp));
                }
                error();
                return null;
            default:
                error();
                return null;



        }
    }

    // Statements -> Statement Statements
    // ->
    private LinkedList<Ast.Stm.T> parseStatements() {
        LinkedList<Ast.Stm.T> stmts = new LinkedList<>();
        while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
                || current.kind == Kind.TOKEN_WHILE
                || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
            stmts.add(parseStatement());
        }
        return stmts;
    }

    // Type -> int []
    // -> boolean
    // -> int
    // -> id
    private Ast.Type.T parseType() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a type.
        switch (current.kind) {
            case TOKEN_INT:
                advance();
                if (current.kind == Kind.TOKEN_LBRACK) {
                    advance();
                    eatToken(Kind.TOKEN_RBRACK);
                    return setLineNumber(new Ast.Type.IntArray());
                }
                return setLineNumber(new Ast.Type.Int());
            case TOKEN_BOOLEAN:
                advance();
                return setLineNumber(new Ast.Type.Boolean());
            case TOKEN_ID:
                String className = current.lexeme;
                advance();
                return setLineNumber(new Ast.Type.ClassType(className));
            default:
                error();
                return null;
        }
    }

    // VarDecl -> Type id ;
    private Ast.Dec.T parseVarDecl() {
        // to parse the "Type" nonterminal in this method, instead of writing
        // a fresh one.
        Ast.Type.T type = parseType();
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_SEMI);
        return setLineNumber(new Ast.Dec.DecSingle(type, id));
    }

    // VarDecls -> VarDecl VarDecls
    // ->
    private LinkedList<Ast.Dec.T> parseVarDecls() {
        LinkedList<Ast.Dec.T> result = new LinkedList<>();
        while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            result.add(parseVarDecl());
        }
        return result;
    }

    // FormalList -> Type id FormalRest*
    // ->
    // FormalRest -> , Type id
    private LinkedList<Ast.Dec.T> parseFormalList() {
        LinkedList<Ast.Dec.T> result = new LinkedList<>();
        if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            Ast.Type.T type = parseType();
            String id = current.lexeme;
            eatToken(Kind.TOKEN_ID);
            result.add(setLineNumber(new Ast.Dec.DecSingle(type, id)));
            while (current.kind == Kind.TOKEN_COMMER) {
                advance();
                type = parseType();
                id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                result.add(setLineNumber(new Ast.Dec.DecSingle(type, id)));
            }
        }
        return result;
    }

    // Method -> public Type id ( FormalList )
    // { VarDecl* Statement* return Exp ;}
    private ast.Ast.Method.T parseMethod() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a method.
        eatToken(Kind.TOKEN_PUBLIC);
        Ast.Type.T retType = parseType();

        String methodName = current.lexeme;
        eatToken(Kind.TOKEN_ID);

        eatToken(Kind.TOKEN_LPAREN);
        LinkedList<Ast.Dec.T> formalList = parseFormalList();
        eatToken(Kind.TOKEN_RPAREN);

        eatToken(Kind.TOKEN_LBRACE);

        LinkedList<Ast.Dec.T> locals = new LinkedList<>();
        LinkedList<Ast.Stm.T> stmts = new LinkedList<>();

        while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            if (current.kind == Kind.TOKEN_ID) {

                String id = current.lexeme;
                advance();
                if (current.kind == Kind.TOKEN_ID) {
                    String varName = current.lexeme;
                    advance();
                    eatToken(Kind.TOKEN_SEMI);

                    locals.add(setLineNumber(new Ast.Dec.DecSingle(setLineNumber(new Ast.Type.ClassType(id)), varName)));
                } else if (current.kind == Kind.TOKEN_ASSIGN) {
                    advance();
                    Ast.Exp.T exp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    stmts.add(setLineNumber(new Ast.Stm.Assign(id, exp)));
                    break;
                } else if (current.kind == Kind.TOKEN_LBRACK) {
                    advance();
                    Ast.Exp.T index = parseExp();
                    eatToken(Kind.TOKEN_RBRACK);
                    eatToken(Kind.TOKEN_ASSIGN);
                    Ast.Exp.T exp = parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                    stmts.add(setLineNumber(new Ast.Stm.AssignArray(id, index, exp)));
                    break;
                } else {
                    error();
                    return null;
                }
            } else {
                locals.add(parseVarDecl());
            }
        }

        stmts.addAll(parseStatements());

        eatToken(Kind.TOKEN_RETURN);
        Ast.Exp.T retExp = parseExp();
        eatToken(Kind.TOKEN_SEMI);

        eatToken(Kind.TOKEN_RBRACE);

        return setLineNumber(new Ast.Method.MethodSingle(retType, methodName, formalList, locals, stmts, retExp));
    }

    // MethodDecls -> MethodDecl MethodDecls
    // ->
    private LinkedList<ast.Ast.Method.T> parseMethodDecls() {
        LinkedList<ast.Ast.Method.T> result = new LinkedList<>();
        while (current.kind == Kind.TOKEN_PUBLIC) {
            result.add(parseMethod());
        }
        return result;
    }

    // ClassDecl -> class id { VarDecl* MethodDecl* }
    // -> class id extends id { VarDecl* MethodDecl* }
    private Ast.Class.T parseClassDecl() {
        eatToken(Kind.TOKEN_CLASS);
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);

        String parentType = null;
        if (current.kind == Kind.TOKEN_EXTENDS) {
            eatToken(Kind.TOKEN_EXTENDS);
            parentType = current.lexeme;
            eatToken(Kind.TOKEN_ID);
        }
        eatToken(Kind.TOKEN_LBRACE);
        LinkedList<Ast.Dec.T> decls = parseVarDecls();
        LinkedList<ast.Ast.Method.T> methodDecls = parseMethodDecls();
        eatToken(Kind.TOKEN_RBRACE);
        return setLineNumber(new Ast.Class.ClassSingle(id, parentType, decls, methodDecls));
    }

    // ClassDecls -> ClassDecl ClassDecls
    // ->
    private LinkedList<Ast.Class.T> parseClassDecls() {
        LinkedList<Ast.Class.T> classes = new LinkedList<>();
        while (current.kind == Kind.TOKEN_CLASS) {
            classes.add(parseClassDecl());
        }
        return classes;
    }

    // MainClass -> class id
    // {
    // public static void main ( String [] id )
    // {
    // Statement
    // }
    // }
    private Ast.MainClass.T parseMainClass() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a main class as described by the
        // grammar above.
        eatToken(Kind.TOKEN_CLASS);
        String className = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LBRACE);
        eatToken(Kind.TOKEN_PUBLIC);
        eatToken(Kind.TOKEN_STATIC);
        eatToken(Kind.TOKEN_VOID);
        eatToken(Kind.TOKEN_MAIN);
        eatToken(Kind.TOKEN_LPAREN);
        eatToken(Kind.TOKEN_STRING);
        eatToken(Kind.TOKEN_LBRACK);
        eatToken(Kind.TOKEN_RBRACK);
        String argsName = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_LBRACE);
        Ast.Stm.T statement = parseStatement();
        eatToken(Kind.TOKEN_RBRACE);
        eatToken(Kind.TOKEN_RBRACE);

        return setLineNumber(new Ast.MainClass.MainClassSingle(className, argsName, statement));
    }

    // Program -> MainClass ClassDecl*
    private ast.Ast.Program.T parseProgram() {
        Ast.MainClass.T mainClass = parseMainClass();
        LinkedList<Ast.Class.T> classes = parseClassDecls();
        eatToken(Kind.TOKEN_EOF);
        return setLineNumber(new Ast.Program.ProgramSingle(mainClass, classes));
    }

    public ast.Ast.Program.T parse() {
        return parseProgram();
    }

    private <X extends BaseAcceptable> X setLineNumber(X x) {
        x.line = current.lineNum;
        return x;
    }
}
