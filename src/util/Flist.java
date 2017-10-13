package util;

public class Flist
{
  public Flist()
  {
  }

  public <X> java.util.LinkedList<X> list(
      @SuppressWarnings("unchecked") X... args)
  {
    java.util.LinkedList<X> list = new java.util.LinkedList<X>();
    for (X arg : args)
      list.addLast(arg);
    return list;
  }
}
