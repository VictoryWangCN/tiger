package elaborator;

import ast.Ast.Type;

import java.util.Hashtable;

public class ClassBinding {
    public String extendss; // null for non-existing extends
    public java.util.Hashtable<String, Type.T> fields;
    public java.util.Hashtable<String, MethodType> methods;

    public ClassBinding(String extendss) {
        this.extendss = extendss;
        this.fields = new Hashtable<String, Type.T>();
        this.methods = new Hashtable<String, MethodType>();
    }

    public ClassBinding(String extendss,
                        java.util.Hashtable<String, Type.T> fields,
                        java.util.Hashtable<String, MethodType> methods) {
        this.extendss = extendss;
        this.fields = fields;
        this.methods = methods;
    }

    public void put(String xid, Type.T type) {
        if (this.fields.get(xid) != null) {
            System.out.println("duplicated class field: " + xid);
            System.exit(1);
        }
        this.fields.put(xid, type);
    }

    public void put(String mid, MethodType mt) {
        if (this.methods.get(mid) != null) {
            System.out.println("duplicated class method: " + mid);
            System.exit(1);
        }
        this.methods.put(mid, mt);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indent) {
        String isr = new String(new char[indent]).replace('\0', ' ');
        StringBuilder sb = new StringBuilder();
        sb.append(isr).append("extends: ");
        if (this.extendss != null)
            sb.append(this.extendss).append('\n');
        else
            sb.append("<>").append('\n');
        sb.append(isr).append("fields:\n  ");
        sb.append(isr).append(isr).append(fields.toString()).append('\n');
        sb.append(isr).append("methods:\n  ");
        sb.append(isr).append(isr).append(methods.toString()).append('\n');

        return sb.toString();

    }

}
