package com.aisleshare;

import java.util.Comparator;

public  class ItemComparator{
    public class Name implements Comparator<Item>{
        public int compare(Item left, Item right) {
            return left.getName().toLowerCase().compareTo(right.getName().toLowerCase());
        }
    }
    public class Type implements Comparator<Item>{
        public int compare(Item left, Item right) {
            return left.getType().toLowerCase().compareTo(right.getType().toLowerCase());
        }
    }
    public class Quantity implements Comparator<Item>{
        public int compare(Item left, Item right) {
            return (int)Math.round(left.getQuantity() - right.getQuantity());
        }
    }
    public class Created implements Comparator<Item>{
        public int compare(Item left, Item right) {
            return left.getCreated() - right.getCreated();
        }
    }
    public class Checked implements Comparator<Item>{
        public int compare(Item left, Item right) {
            int leftInt, rightInt;
            if(left.getChecked()) leftInt = 1;
            else leftInt = 0;
            if(right.getChecked()) rightInt = 1;
            else rightInt = 0;
            return leftInt - rightInt;
        }
    }
}


