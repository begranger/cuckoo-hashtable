package CuckooHashTable;

import CuckooHashTable.errors.EntryNotFoundError;

import java.util.*;


// TKey = class/type of key
// TValue = class/type of object to be stored in the hashtable
public class CuckooHashTable<TKey, TValue> implements Iterable<CuckooHashTable.HashEntry<TKey,TValue>> {



    /* MEMBER VALUES */

    private ArrayList<HashEntry<TKey, TValue>> hashArray1;
    private ArrayList<HashEntry<TKey, TValue>> hashArray2;
    // keeps track of each tables size
    private int sizeOf1;
    private int sizeOf2;
    // int to keep track of how many times insert loop has run,
    // gets reset after each insert--saves me trouble of passing as param
    private int insertCounter;
    // the hashCodes themselves remain the same but, when they return the
    // code, they are divided by different numbers, stored here, so they
    // can be updated/changed
    private int hashDivide1;
    private int hashDivide2;


    // enum to keep track of entry status
    private enum EntryStatus { ACTIVE, EMPTY, DELETED }


    // private iterator class to iterate through hash table
    private class HashIterator implements Iterator<CuckooHashTable.HashEntry<TKey, TValue>> {


        /* PRIVATE MEMBERS */

        // ints to keep track of our current index in array
        private int index;

        // int to keep track of how many entries we've gone through and how
        // many we have left
        private int entriesProcessed;


        // to make things easier here, we combine both arrays into one, since
        // all we care about in the iterator is having them, not where they are
        private ArrayList<HashEntry<TKey, TValue>> toIterate;


        /* STANDARD CONSTRUCTOR */
        public HashIterator() {
            toIterate = new ArrayList<>(hashArray1);
            toIterate.addAll(hashArray2);

            entriesProcessed = 0;
            // had to start this at -1 because next() increments index before checking
            // for EMPTY
            index = -1;

        }



        /* OVERRIDES */

        @Override
        public boolean hasNext()
        {
            return !(entriesProcessed == size());
        }


        @Override
        public HashEntry<TKey, TValue> next()
        {
            // increments one to get past previous entry
            index++;

            while (toIterate.get(index).status == EntryStatus.EMPTY)
            {
                index++;
            }

            entriesProcessed++;

            return toIterate.get(index);
        }


        @Override
        // you cant do this
        public void remove() throws IllegalStateException
        {
            if (toIterate.get(index).status == EntryStatus.EMPTY)
            {
                throw new IllegalStateException();
            }
            else
            {
                CuckooHashTable.this.remove(toIterate.get(index).m_key);
            }

        }
    }


    // nested HashEntry class, see--
    // http://stackoverflow.com/questions/30699826/java-how-to-create-custom-hashtable-iterator
    public static class HashEntry<TKey, TValue>
    {

        /* PRIVATE MEMBERS */
        private TKey m_key;
        private TValue m_value;
        private EntryStatus status;




        /* CONSTRUCTORS */

        // standard constructor
        private HashEntry(TKey key, TValue value)
        {
            m_key = key;
            m_value = value;
            status = EntryStatus.ACTIVE;
        }


        private HashEntry(TKey key, TValue value, EntryStatus i) {
            m_key = key;
            m_value = value;
            status = i;
        }


        // default 'empty' constructor
        private HashEntry()
        {
            // calls default constructor, creates placeholder entry
            m_key = null;
            m_value = null;
            status = EntryStatus.EMPTY;
        }




        /* OVERRIDES */

        // equals operator override, this override just compares compares
        // the objects held in the entry, so any object used with this
        // implementation must hae=ve its own equals override
        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) { return false; }
            if (getClass() != obj.getClass()) { return false; }

            final HashEntry other = (HashEntry) obj;
            return (!((this.m_key == null) ? (other.m_key != null) : !this.m_key.equals(other.m_key)));
        }


        // override of the hashCode() function--just calls the hashCode
        // function of the embedded object, so that must be provided
        @Override
        public int hashCode()
        {
            return this.m_key.hashCode();
        }


        // toString override just returns the toString of the embedded object
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            // handles NullPointerException
            if (m_key == null || m_value == null)
            {
                sb.append("Empty entry.");
            }
            else
            {
                sb.append(m_key.toString()).append(": ").append(m_value.toString()).append("\n");
            }

            return sb.toString();

        }




        /* GETTERS */
        public TKey getKey()
        {
            return m_key;
        }


        public TValue getValue()
        {
            return m_value;
        }


        public EntryStatus getEntryStatus()
        {
            return status;
        }
    }




    /* STANDARD CONSTRUCTOR */
    // since we have nothing to put in the
    // entries yet, we just give them empty Objects
    public CuckooHashTable()
    {
        // initializes both hash tables to 3 (prime) 'empty' hashEntries
        hashArray1 = new ArrayList<>(Arrays.asList(new HashEntry<TKey, TValue>(), new HashEntry<TKey, TValue>(), new HashEntry<TKey, TValue>()));
        hashArray2 = new ArrayList<>(Arrays.asList(new HashEntry<TKey, TValue>(), new HashEntry<TKey, TValue>(), new HashEntry<TKey, TValue>()));

        hashDivide1 = 1;
        hashDivide2 = 11;

        sizeOf1 = 0;
        sizeOf2 = 0;

        insertCounter = 0;
    }



    /* OVERRIDES */
    @Override
    public boolean equals(Object obj)
    {
        if (this.getClass() != obj.getClass())
        {
            return false;
        }

        final CuckooHashTable rvalue = (CuckooHashTable) obj;

        return (this.hashArray1 == rvalue.hashArray1 && this.hashArray2 == rvalue.hashArray2);
    }


    @Override
    public int hashCode()
    {
        return (int) Math.floor((hashArray1.hashCode() + hashArray2.hashCode())/2);
    }


    // override of the toString method returns ALL elements in the table by calling
    // the toString method of every object in each ACTIVE hash entry
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (HashEntry<TKey, TValue> entry : this)
        {
            sb.append(entry.toString());
        }

        return sb.toString();
    }


    // iterator override
    @Override
    public Iterator<HashEntry<TKey, TValue>> iterator() {
        return new HashIterator();
    }




    /* PUBLIC METHODS */

    public int size() { return sizeOf1 + sizeOf2; }


    // see above notes on contains()
    public boolean contains(TKey key)
    {
        // if its not in the first pos we check the second.
        // if its not there, we return false
        return (inOne(key) || inTwo(key));
    }


    public void makeEmpty()
    {
        // iterates through each table marking the status of
        // all entries as EMPTY
        sizeOf1 = 0;
        sizeOf2 = 0;

        for (HashEntry entry : hashArray1)
        {
            entry.status = EntryStatus.EMPTY;
        }

        for (HashEntry entry : hashArray2)
        {
            entry.status = EntryStatus.EMPTY;
        }
    }


    public boolean insert(TKey key, TValue value)
    {
        // checks to make sure object isn't already in the tables
        if (contains(key)) { return false; }

        // otherwise, we insert the object at its first position, and begin
        // cuckoo loop
        return CuckooInsert(key, value);
    }


    public boolean remove(TKey key)
    {
        if(!inOne(key))
        {
            if (!inTwo(key))
            {
                return false;
            }
            else
            {
                hashArray2.get(findPos2(key)).status = EntryStatus.DELETED;
                return true;
            }
        }
        else
        {
            hashArray1.get(findPos1(key)).status = EntryStatus.DELETED;
            return true;
        }
    }


    // returns object in Hash Entry
    public HashEntry<TKey, TValue> getEntry(TKey key) throws EntryNotFoundError
    {
        // checks to make sure that the requested entry is in the table
        if (!contains(key)) {
            throw new EntryNotFoundError();
        }
        else
        {
            if (inOne(key))
            {
                return hashArray1.get(findPos1(key));
            }
            else
            {
                return hashArray2.get(findPos2(key));
            }
        }

    }




    /* PRIVATE METHODS */

    private int findPos1(TKey key)
    {
        int hashVal = hash(key);

        hashVal = (int) ((Math.floor(hashVal/hashDivide1)) % hashArray1.size());
        if (hashVal < 0)
        {
            hashVal += hashArray1.size();
        }
        return hashVal;
    }


    private int findPos2(TKey key)
    {
        int hashVal = hash(key);

        hashVal = (int) ((Math.floor(hashVal/hashDivide2)) % hashArray2.size());
        if (hashVal < 0)
        {
            hashVal += hashArray2.size();
        }
        return hashVal;
    }


    // hash functions--return number from object--note this only calls
    // the hashCode function of the object passed in, so it must have
    // an overridden hashCode() function, or the default will be used
    private int hash(TKey key)
    {
        return key.hashCode();
    }


    // internal rehashing method--makes new hash functions and tables
    private void rehash()
    {
        Random randNum = new Random();

        // creates new hash functions
        hashDivide1 = nextPrime(randNum.nextInt() + 1);
        hashDivide2 = nextPrime(randNum.nextInt() + 1);

        // creates new tables, sizes should be same in old ones
        ArrayList<HashEntry<TKey, TValue>> newList1 = new ArrayList<>(nextPrime(hashArray1.size() * 2));
        ArrayList<HashEntry<TKey, TValue>> newList2 = new ArrayList<>(nextPrime(hashArray2.size() * 2));

        // fills the arrays with 'empty' entries--only happens once--get over it
        for (int i = 0; i < newList1.size(); i++)
        {
            newList1.set(i, new HashEntry<TKey, TValue>());
            newList2.set(i, new HashEntry<TKey, TValue>());
        }

        // stores old arrays
        ArrayList<HashEntry<TKey, TValue>> oldArray1 = hashArray1;
        ArrayList<HashEntry<TKey, TValue>> oldArray2 = hashArray2;

        // sets new arrays as the 'in-service' ones
        hashArray1 = newList1;
        hashArray2 = newList2;

        // resets sizes
        sizeOf1 = 0;
        sizeOf2 = 0;

        // copies the tables over, only need one loop since sizes should be same
        for (int i = 0; i < oldArray1.size(); i++)
        {
            if (oldArray1.get(i).status == EntryStatus.ACTIVE)
            {
                insert(oldArray1.get(i).m_key, oldArray1.get(i).m_value);
            }
            if (oldArray2.get(i).status == EntryStatus.ACTIVE)
            {
                insert(oldArray2.get(i).m_key, oldArray2.get(i).m_value);
            }
        }

    }


    // internal cuckoo insert implementations
    private boolean CuckooInsert(TKey key, TValue value)
    {
        // runs until the insert succeeds
        while (!insertInOne(key, value))
        {
            rehash();
        }

        // resets insertCounter
        insertCounter = 0;

        return true;
    }


    private boolean insertInOne(TKey key, TValue value)
    {

        int pos1 = findPos1(key);

        if (insertCounter == 16)
        {
            return false;
        }
        else
        {
            // if the index in array 1 is not empty, we have to evict
            // whats there
            if (hashArray1.get(pos1).status != EntryStatus.EMPTY) {
                insertInTwo(hashArray1.get(pos1).m_key, hashArray1.get(pos1).m_value);
            }
            else
            {
                sizeOf1++;
            }

            hashArray1.set(pos1, new HashEntry<>(key, value));
            return true;
        }
    }


    private boolean insertInTwo(TKey key, TValue value)
    {
        int pos2 = findPos2(key);

        if (insertCounter == 16)
        {
            return false;
        }
        else
        {
            // if the index in array 1 is not empty, we have to evict
            // whats there
            if (hashArray2.get(pos2).status != EntryStatus.EMPTY) {
                insertInOne(hashArray2.get(pos2).m_key, hashArray2.get(pos2).m_value);
            }
            else
            {
                sizeOf2++;
            }

            hashArray2.set(pos2, new HashEntry<>(key, value));
            return true;
        }
    }


    // internal methods to check if an entry is already
    // in the table
    private boolean inOne(TKey key)
    {
        int keyPos = findPos1(key);
        return (hashArray1.get(keyPos).m_key == key);
    }


    private boolean inTwo(TKey key)
    {
        int keyPos = findPos2(key);
        return (hashArray2.get(keyPos).m_key == key);
    }


    // quick methods to get next primes for table resizing
    private int nextPrime(int x)
    {
        if (x <= 0) { x = 3; }
        if ( x % 2 == 0 ) { x++; }

        while (!isPrime(x)) { x += 2; }

        return x;
    }


    private boolean isPrime(int x)
    {
        if ( x == 2 || x == 3) { return true; }
        if ( x == 1 || x % 2 == 0) { return false; }

        for (int i = 3; i * i <= x; i += 2)
        {
            if (x % i == 0) { return false; }
        }
        return true;
    }


}























