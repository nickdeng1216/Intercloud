package hk.edu.polyu.intercloud.api;

import java.util.Arrays;
import java.util.List;

public class TestAPI {

	public void print() {
		System.out.println("What do you want to print?");
	}

	public void print(long id, String s) {
		System.out.println(s);
	}

	public void print(long id, long l) {
		System.out.println(l + " (long)");
	}

	public void print(long id, double d) {
		System.out.println(d + " (double)");
	}

	public void print(long id, String s, long l) {
		System.out.println(l + " " + s);
	}

	public void print(long id, boolean b) {
		System.out.println(b + " (bool)");
	}

	public void print(long id, List<String> list) {
		System.out.println(Arrays.toString(list.toArray()));
	}
}
