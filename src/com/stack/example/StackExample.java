package com.stack.example;

import java.util.Stack;

public class StackExample {

    public static void main(String[] args) {

        Stack<String> stack = new Stack<>();

        
        System.out.println("Is Stack empty?: " + stack.isEmpty());

        stack.push("Skyrim");
        stack.push("Doom");
        stack.push("Borderlands");

        System.out.println(stack);
        System.out.println("Is Stack empty?: " + stack.isEmpty());

    }

}
