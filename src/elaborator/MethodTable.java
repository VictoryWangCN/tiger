package elaborator;

import ast.Ast.Dec;
import ast.Ast.Type;

import java.util.LinkedList;

public class MethodTable {
    private java.util.Hashtable<String, Type.T> table;

    public MethodTable() {
        this.table = new java.util.Hashtable<String, Type.T>();
    }

    // Duplication is not allowed
    public void put(LinkedList<Dec.T> formals,
                    LinkedList<Dec.T> locals) {
        for (Dec.T dec : formals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.table.get(decc.id) != null) {
                System.out.println("duplicated parameter: " + decc.id);
                System.exit(1);
            }
            this.table.put(decc.id, decc.type);
        }

        for (Dec.T dec : locals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.table.get(decc.id) != null) {
                System.out.println("duplicated variable: " + decc.id);
                System.exit(1);
            }
            this.table.put(decc.id, decc.type);
        }

    }

    // return null for non-existing keys
    public Type.T get(String id) {
        return this.table.get(id);
    }

    public void dump() {
        System.out.println(toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.table.forEach((s, t) -> {
            sb.append(s).append(": ").append(t).append('\n');


        });
        return sb.toString();
    }
}
