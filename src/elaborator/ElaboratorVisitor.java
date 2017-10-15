package elaborator;

import ast.Ast.Class;
import ast.Ast.Class.ClassSingle;
import ast.Ast.*;
import ast.Ast.Exp.*;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm.*;
import ast.Ast.Type.ClassType;
import control.Control.ConAst;

import java.util.LinkedList;

public class ElaboratorVisitor implements ast.Visitor {
    public ClassTable classTable; // symbol table for class
    public MethodTable methodTable; // symbol table for each method
    public String currentClass; // the class name being elaborated
    public Type.T type; // type of the expression being elaborated

    public ElaboratorVisitor() {
        this.classTable = new ClassTable();
        this.methodTable = new MethodTable();
        this.currentClass = null;
        this.type = null;
    }


    private void checkType(Type.T except) {
        if (this.type != null) {
            checkType(except, this.type);
        }
    }

    private void checkType(Type.T except, Type.T actual) {
        if (except.getClass() != actual.getClass()) {
            System.err.println("Except Type: " + except);
            System.err.println("Actual Type: " + actual);
        }
    }

    private void undefinedReference(int line, String id) {
        System.err.println("Undefined reference to [" + id + "] at line " + line);
    }

    private void wrongArgsNumber(String id, String mid, int except, int actual, int line){
        System.err.printf("Invoke method [%s.%s] with wrong arguments size at line [%d]: except [%d], actual [%d]\n", id, mid, line, except, actual);
    }

    private void wrongArgType(String id, String mid, Type.T except, Type.T actual, int line) {
        System.err.printf("Invoke method [%s] with wrong argument[%s] type at line [%d]: except [%s], actual [%s]\n", id, mid, line, except, actual);

    }

    // /////////////////////////////////////////////////////
    // expressions
    @Override
    public void visit(Add e) {
        e.left.accept(this);
        Type.T left = this.type;

        e.right.accept(this);
        checkType(left);

        this.type = new Type.Int();
    }

    @Override
    public void visit(And e) {
        e.left.accept(this);
        checkType(Type.Boolean.INSTANCE);

        e.right.accept(this);
        checkType(Type.Boolean.INSTANCE);

        this.type = new Type.Boolean();
    }

    @Override
    public void visit(ArraySelect e) {
        e.array.accept(this);
        checkType(Type.IntArray.INSTANCE);

        e.index.accept(this);
        checkType(Type.Int.INSTANCE);

        this.type = new Type.Int();
    }

    @Override
    public void visit(Call e) {
        Type.T leftty;
        Type.ClassType ty = null;

        e.exp.accept(this);
        leftty = this.type;

        if (leftty == null) return;
        checkType(new ClassType(""));
        ty = (ClassType) leftty;
        e.type = ty.id;


        MethodType mty = this.classTable.getm(ty.id, e.id);
        java.util.LinkedList<Type.T> argsty = new LinkedList<Type.T>();
        for (Exp.T a : e.args) {
            a.accept(this);
            argsty.addLast(this.type);
        }
        if (mty.argsType.size() != argsty.size()) {
            wrongArgsNumber(ty.id, e.id, mty.argsType.size(), argsty.size(), e.line);
            return;
        }
        for (int i = 0; i < argsty.size(); i++) {
            Dec.DecSingle dec = (Dec.DecSingle) mty.argsType.get(i);
            if (argsty.get(i) != null) {
                if (dec.type.toString().equals(argsty.get(i).toString()))
                    ;
                else
                    wrongArgType(ty.id, e.id, dec.type, argsty.get(i), e.line);
            }
        }
        this.type = mty.retType;
        e.at = argsty;
        e.rt = this.type;
        return;
    }

    @Override
    public void visit(False e) {
        this.type = new Type.Boolean();
    }

    @Override
    public void visit(Id e) {
        // first look up the id in method table
        Type.T type = this.methodTable.get(e.id);
        // if search failed, then s.id must be a class field.
        if (type == null) {
            type = this.classTable.get(this.currentClass, e.id);
            // mark this id as a field id, this fact will be
            // useful in later phase.
            e.isField = true;
        }
        if (type == null) {
            undefinedReference(e.line, e.id);
        }
        this.type = type;
        // record this type on this node for future use.
        e.type = type;
        return;
    }

    @Override
    public void visit(Length e) {
        e.array.accept(this);
        checkType(Type.IntArray.INSTANCE);

        this.type = new Type.Int();
    }

    @Override
    public void visit(Lt e) {
        e.left.accept(this);
        Type.T ty = this.type;
        e.right.accept(this);
        checkType(ty);
        this.type = new Type.Boolean();
        return;
    }

    @Override
    public void visit(NewIntArray e) {
        this.type = new Type.IntArray();
    }

    @Override
    public void visit(NewObject e) {
        this.type = new Type.ClassType(e.id);
        return;
    }

    @Override
    public void visit(Not e) {
        e.exp.accept(this);
        checkType(Type.Boolean.INSTANCE);
    }

    @Override
    public void visit(Num e) {
        this.type = new Type.Int();
        return;
    }

    @Override
    public void visit(Sub e) {
        e.left.accept(this);
        Type.T leftty = this.type;
        e.right.accept(this);
        checkType(leftty);
        this.type = new Type.Int();
        return;
    }

    @Override
    public void visit(This e) {
        this.type = new Type.ClassType(this.currentClass);
        return;
    }

    @Override
    public void visit(Times e) {
        e.left.accept(this);
        Type.T leftty = this.type;
        e.right.accept(this);
        checkType(leftty);
        this.type = new Type.Int();
        return;
    }

    @Override
    public void visit(True e) {
        this.type = new Type.Boolean();
    }

    // statements
    @Override
    public void visit(Assign s) {
        // first look up the id in method table
        Type.T type = this.methodTable.get(s.id);
        // if search failed, then s.id must
        if (type == null)
            type = this.classTable.get(this.currentClass, s.id);
        s.exp.accept(this);
        if (type == null)
            undefinedReference(s.line, s.id);
        else {
            s.type = type;
            checkType(type);
        }
        return;
    }

    @Override
    public void visit(AssignArray s) {
        // first look up the id in method table
        Type.T type = this.methodTable.get(s.id);
        // if search failed, then s.id must
        if (type == null)
            type = this.classTable.get(this.currentClass, s.id);

        if (type == null)
            undefinedReference(s.line, s.id);
        else
            checkType(Type.IntArray.INSTANCE, type);

        s.index.accept(this);
        checkType(Type.Int.INSTANCE);

        s.exp.accept(this);
        checkType(Type.Int.INSTANCE);
    }

    @Override
    public void visit(Block s) {
        s.stms.forEach(f -> f.accept(this));
    }

    @Override
    public void visit(If s) {
        s.condition.accept(this);
        checkType(Type.Boolean.INSTANCE);
        s.thenn.accept(this);
        s.elsee.accept(this);
        return;
    }

    @Override
    public void visit(Print s) {
        s.exp.accept(this);
        checkType(Type.Int.INSTANCE);
        return;
    }

    @Override
    public void visit(While s) {
        s.condition.accept(this);
        checkType(Type.Boolean.INSTANCE);

        s.body.accept(this);
    }

    // type
    @Override
    public void visit(Type.Boolean t) {
    }

    @Override
    public void visit(Type.ClassType t) {
    }

    @Override
    public void visit(Type.Int t) {
    }

    @Override
    public void visit(Type.IntArray t) {
    }

    // dec
    @Override
    public void visit(Dec.DecSingle d) {
        this.classTable.put(this.currentClass, d.id, d.type);
    }

    // method
    @Override
    public void visit(Method.MethodSingle m) {
        // construct the method table
        this.methodTable = new MethodTable();
        this.methodTable.put(m.formals, m.locals);


        if (ConAst.elabMethodTable)
            System.out.println("Method: " + m.id);
            this.methodTable.dump();

        for (Stm.T s : m.stms)
            s.accept(this);
        m.retExp.accept(this);

        this.methodTable.getUnusedVars().forEach((s, t) -> {
            System.err.printf("Warn: variable \"%s\" declared at line %d never used\n", s, t.line);
        });

        return;
    }

    // class
    @Override
    public void visit(Class.ClassSingle c) {
        this.currentClass = c.id;

        for (Method.T m : c.methods) {
            m.accept(this);
        }
        return;
    }

    // main class
    @Override
    public void visit(MainClass.MainClassSingle c) {
        this.currentClass = c.id;
        // "main" has an argument "arg" of type "String[]", but
        // one has no chance to use it. So it's safe to skip it...

        c.stm.accept(this);
        return;
    }

    // ////////////////////////////////////////////////////////
    // step 1: build class table
    // class table for Main class
    private void buildMainClass(MainClass.MainClassSingle main) {
        this.classTable.put(main.id, new ClassBinding(null));
    }

    // class table for normal classes
    private void buildClass(ClassSingle c) {
        this.classTable.put(c.id, new ClassBinding(c.extendss));
        for (Dec.T dec : c.decs) {
            Dec.DecSingle d = (Dec.DecSingle) dec;
            this.classTable.put(c.id, d.id, d.type);
        }
        for (Method.T method : c.methods) {
            MethodSingle m = (MethodSingle) method;
            this.classTable.put(c.id, m.id, new MethodType(m.retType, m.formals));
        }
    }

    // step 1: end
    // ///////////////////////////////////////////////////

    // program
    @Override
    public void visit(ProgramSingle p) {
        // ////////////////////////////////////////////////
        // step 1: build a symbol table for class (the class table)
        // a class table is a mapping from class names to class bindings
        // classTable: className -> ClassBinding{extends, fields, methods}
        buildMainClass((MainClass.MainClassSingle) p.mainClass);
        for (Class.T c : p.classes) {
            buildClass((ClassSingle) c);
        }

        // we can double check that the class table is OK!
        if (control.Control.ConAst.elabClassTable) {
            this.classTable.dump();
        }

        // ////////////////////////////////////////////////
        // step 2: elaborate each class in turn, under the class table
        // built above.
        p.mainClass.accept(this);
        for (Class.T c : p.classes) {
            c.accept(this);
        }

    }
}
