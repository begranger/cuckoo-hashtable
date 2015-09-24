package CuckooHashTable.errors;

public class EntryNotFoundError extends Exception {

   public EntryNotFoundError()
   {
       super("The entry you have requested does not exist");
   }

}
