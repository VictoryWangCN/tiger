package ast;

import ast.Ast.Class.ClassSingle;
import ast.Ast.*;
import ast.Ast.Exp.*;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Stm.*;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;

import java.util.Iterator;

public class PrettyPrintVisitor implements Visitor {
    private int indentLevel;

    public PrettyPrintVisitor() {
        this.indentLevel = 4;
    }

    private void indent() {
        this.indentLevel += 2;
    }

    private void unIndent() {
        this.indentLevel -= 2;
    }

    private void printSpaces() {
        int i = this.indentLevel;
        while (i-- != 0)
            this.say(" ");
    }

    private void sayln(String s) {
        System.out.println(s);
    }

    private void say(String s) {
        System.out.print(s);
    }

    // /////////////////////////////////////////////////////
    // expressions
    @Override
    public void visit(Add e) {
        // Lab2, exercise4: filling in missing code.
        // Similar for other methods with empty bodies.
        // Your code here:
        e.left.accept(this);
        this.say(" + ");
        e.right.accept(this);
    }

    @Override
    public void visit(And e) {
    }

    @Override
    public void visit(ArraySelect e) {
    }

    @Override
    public void visit(Call e) {
        e.exp.accept(this);
        this.say("." + e.id + "(");
        Iterator<Exp.T> iterator = e.args.iterator();
        if (iterator.hasNext()) {
            Exp.T x = iterator.next();
            x.accept(this);
        }

        while (iterator.hasNext()) {
            this.say(", ");
            Exp.T x = iterator.next();
            x.accept(this);
        }
        this.say(")");
        return;
    }

    @Override
    public void visit(False e) {
    }

    @Override
    public void visit(Id e) {
        this.say(e.id);
    }

    @Override
    public void visit(Length e) {
    }

    @Override
    public void visit(Lt e) {
        e.left.accept(this);
        this.say(" < ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(NewIntArray e) {
    }

    @Override
    public void visit(NewObject e) {
        this.say("new " + e.id + "()");
        return;
    }

    @Override
    public void visit(Not e) {
    }

    @Override
    public void visit(Num e) {
        System.out.print(e.num);
        return;
    }

    @Override
    public void visit(Sub e) {
        e.left.accept(this);
        this.say(" - ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(This e) {
        this.say("this");
    }

    @Override
    public void visit(Times e) {
        e.left.accept(this);
        this.say(" * ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(True e) {
    }

    // statements
    @Override
    public void visit(Assign s) {
        this.printSpaces();
        this.say(s.id + " = ");
        s.exp.accept(this);
        this.say(";");
        return;
    }

    @Override
    public void visit(AssignArray s) {
    }

    @Override
    public void visit(Block s) {
        this.printSpaces();
        this.sayln("{");
        this.indent();
        s.stms.forEach(st -> {
            st.accept(this);
            this.sayln("");
        });
        this.unIndent();
        this.printSpaces();
        this.sayln("}");

    }

    @Override
    public void visit(If s) {
        this.printSpaces();
        this.say("if (");
        s.condition.accept(this);
        this.sayln(")");
        this.indent();
        s.thenn.accept(this);
        this.unIndent();
        this.sayln("");
        this.printSpaces();
        this.sayln("else");
        this.indent();
        s.elsee.accept(this);
        this.sayln("");
        this.unIndent();
        return;
    }

    @Override
    public void visit(Print s) {
        this.printSpaces();
        this.say("System.out.println (");
        s.exp.accept(this);
        this.sayln(");");
        return;
    }

    @Override
    public void visit(While s) {
        this.printSpaces();
        this.say("while (");
        s.condition.accept(this);
        this.sayln(")");
        s.body.accept(this);
        return;
    }

    // type
    @Override
    public void visit(Boolean t) {
    }

    @Override
    public void visit(ClassType t) {
    }

    @Override
    public void visit(Int t) {
        this.say("int");
    }

    @Override
    public void visit(IntArray t) {
    }

    // dec
    @Override
    public void visit(Dec.DecSingle d) {
    }

    // method
    @Override
    public void visit(MethodSingle m) {
        this.say("  public ");
        m.retType.accept(this);
        this.say(" " + m.id + "(");
        Iterator<Dec.T> it = m.formals.iterator();
        if (it.hasNext()) {
            Dec.T d = it.next();
            Dec.DecSingle dec = (Dec.DecSingle) d;
            dec.type.accept(this);
            this.say(" " + dec.id);
        }
        while (it.hasNext()) {
            Dec.T d = it.next();
            Dec.DecSingle dec = (Dec.DecSingle) d;
            dec.type.accept(this);
            this.say(", " + dec.id);
        }
        this.sayln(")");
        this.sayln("  {");

        for (Dec.T d : m.locals) {
            Dec.DecSingle dec = (Dec.DecSingle) d;
            this.printSpaces();
            dec.type.accept(this);
            this.sayln(" " + dec.id + ";");
        }
        this.sayln("");
        for (Stm.T s : m.stms) {
            s.accept(this);
            this.sayln("");

        }
        this.printSpaces();
        this.say("return ");
        m.retExp.accept(this);
        this.sayln(";");
        this.sayln("  }");
        return;
    }

    // class
    @Override
    public void visit(ClassSingle c) {
        this.say("class " + c.id);
        if (c.extendss != null)
            this.sayln(" extends " + c.extendss);
        else
            this.sayln("");

        this.sayln("{");

        for (Dec.T d : c.decs) {
            Dec.DecSingle dec = (Dec.DecSingle) d;
            this.say("  ");
            dec.type.accept(this);
            this.say(" ");
            this.sayln(dec.id + ";");
        }
        for (Method.T mthd : c.methods)
            mthd.accept(this);
        this.sayln("}");
        return;
    }

    // main class
    @Override
    public void visit(MainClass.MainClassSingle c) {
        this.sayln("class " + c.id);
        this.sayln("{");
        this.sayln("  public static void main (String [] " + c.arg + ")");
        this.sayln("  {");
        c.stm.accept(this);
        this.sayln("  }");
        this.sayln("}");
        return;
    }

    // program
    @Override
    public void visit(Program.ProgramSingle p) {
        p.mainClass.accept(this);
        this.sayln("");
        for (ast.Ast.Class.T classs : p.classes) {
            classs.accept(this);
        }
        System.out.println("\n\n");
    }
}
