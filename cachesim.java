import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class cachesim {
	public static class Block{
		int start;
		String data; 
		int dirtyBit;
		int valid;
		public Block()
		{
			valid = 0;
			dirtyBit = 0;
		}
		public Block(String data, int start)
		{
			this.start = start;
			this.data = data;
			dirtyBit = 1;
			valid = 1;
		}
		public void setDirty()
		{
			dirtyBit = 1;
		}
	}
	public static void main(String[] args) throws FileNotFoundException
	{
		String file = args[0]; //name of source file
		int cacheSize = Integer.parseInt(args[1]); //size of cache in KB
		int associativity = Integer.parseInt(args[2]); //set associativity
		String type = args[3]; //writeback-writeallocate or writethrough-writenoallocate
		int blockSize = Integer.parseInt(args[4]); //size of block in bytes
		int numFrames = cacheSize*1024 / blockSize;
		int numSets = numFrames/associativity;
		int mainSize = 16777216; //2^24
	
		String[] mem = new String[mainSize]; //allocates space for memory
		for(int i = 0; i<mainSize; i++)
		{
			mem[i] = "00"; //fills each address with a byte
		}
		/**********new**********/
		List<LinkedList<Block>> cach = new ArrayList<LinkedList<Block>>(); //creates cache
		for(int j = 0; j<numSets; j++)
		{
			cach.add(new LinkedList<Block>()); //creating arraylist of empty sets
		} //not full set means there is an invalid block that can be used
		
		
		Scanner scan = new Scanner(new File(file));
		
		while(scan.hasNext())
		{
			String loadStore = scan.next();
			String adr;
			int address;
			int bytes;
			String contents;
			String temp; //for block contents
			int setNum;
			
			Block tt = new Block();
			
			if(loadStore.equalsIgnoreCase("store")) //if store
			{
				if(type.equals("wt")) //write through
				{
					adr = scan.next(); //address in hex
					address = Integer.parseInt(adr.substring(2), 16); //address in dec
					bytes = Integer.parseInt(scan.next()); //how many bytes
					contents = scan.next(); //what is to be stored
					
					System.out.print("store " + adr + " ");
					
					setNum = (address/blockSize) % numSets; //find which set it belongs to
					boolean found = false;
					if(cach.get(setNum).size() != 0) //if there are blocks, check for hit
					{
						int find = address%blockSize;
						int first = address-find; //first address within a block
						for(Block t : cach.get(setNum)) //go through each block
						{
							if(t.start == first) //if matching address
							{
								System.out.println("hit");
								found = true;
								
								/*******updating cache**********/
								temp = t.data; //start of block with address
								String f = temp.substring(0,find*2);
								if (temp.length() <= find*2+bytes*2)
									temp = f + contents;
								else
									temp = f + contents + temp.substring(find*2+bytes*2);
								t.data = temp;
								tt = new Block(temp,first);
								tt.dirtyBit = 0;
								cach.get(setNum).remove(t); //remove
								cach.get(setNum).addLast(tt); //add it to the back
								/*******************************/
							}
						}
					}

					if (found == false)
					{
						System.out.println("miss");
					}
					//write through to main memory
					int start = 0;
					int end = 2;
					/******adding to main memory*******/
					for(int i = address; i<address+bytes; i++)
					{
						mem[i] = contents.substring(start, end);
						start+=2;
						end+=2;
					}
					/*****************************************/
					
				}
				else //write back
				{
					adr = scan.next(); //address in hex
					address = Integer.parseInt(adr.substring(2), 16); //address in dec
					bytes = Integer.parseInt(scan.next()); //how many bytes
					contents = scan.next(); //what is to be stored
					
					System.out.print("store " + adr + " ");
					
					setNum = (address/blockSize) % numSets;
					boolean found = false;
					if(cach.get(setNum).size() != 0)
					{ //just like write through,but set dirty bit to 1 
						int find = address%blockSize;
						int first = address-find;
						for(Block t : cach.get(setNum))
						{
							if(t.start == first)
							{
								System.out.println("hit");
								found = true;
								
								/*******updating cache**********/
								temp = t.data; //start of block with address
								String f = temp.substring(0,find*2);
								if (temp.length() <= find*2+bytes*2)
									temp = f + contents;
								else
									temp = f + contents + temp.substring(find*2+bytes*2);
								t.data = temp;
								t.dirtyBit = 1;
								tt = new Block(temp,first);
								cach.get(setNum).remove(t);
								cach.get(setNum).addLast(tt);
								/*******************************/
							}
						}
					}

					if (found == false)
					{
						System.out.println("miss");
						int find = address%blockSize;
						int first = address-find;
						if (cach.get(setNum).size() >= associativity)
						{
							//if misses and full ---> kick least recently used load it to main memory
							/******adding from cache to main memory*******/
							tt = cach.get(setNum).removeFirst(); //get cache value for block
							if(tt.dirtyBit != 0) //only write to memory if necessary 
							{
								int ret = tt.start;
								int start = 0;
								int end = 2;
								for(int i = ret; i<ret+tt.data.length()/2; i++)
								{
									mem[i] = tt.data.substring(start, end); //put it in memory at block number
									start+=2;
									end+=2;
								}
							}
				
						}	
						//means there's an open slot
						/***********get main memory write to cache*************/
						temp = mem[first]; //start address of block
						for(int i = first+1; i<first+blockSize; i++)
							temp+=mem[i]; //fill block space with original address
						String f = temp.substring(0,find*2);
						if (temp.length() <= find*2+bytes*2) //alter address
							temp = f + contents;
						else
							temp = f + contents + temp.substring(find*2+bytes*2);
						tt = new Block(temp, first); //adds updated info - dirty bit is 1
						cach.get(setNum).addLast(tt); //puts new info at end of cache linked list
						
					}					
				}
			}
			else //if load
			{
				adr = scan.next(); //address in hex
				address = Integer.parseInt(adr.substring(2), 16); //address in dec
				bytes = Integer.parseInt(scan.next()); //how many bytes
				
				System.out.print("load " + adr + " ");
				
				setNum = (address/blockSize) % numSets;
				boolean found = false;
				if(cach.get(setNum).size() != 0)
				{
					int find = address%blockSize;
					int first = address-find;
					for(Block t : cach.get(setNum))
					{
						if(t.start == first) //if it's a hit, update the cache
						{
							System.out.print("hit ");
							temp = t.data;
							if (find*2+bytes*2 < temp.length())
								System.out.println(temp.substring(find*2, find*2+bytes*2));
							else
								System.out.println(temp.substring(find*2));
							found = true;
							break;
						}
					}
				}
				if(found == false)
				{
					System.out.print("miss ");
					int find = address%blockSize;
					int first = address-find;
					if(cach.get(setNum).size() >= associativity)
					{
						//if misses and full ---> kick least recently used load it to main memory
						/******adding from cache to main memory*******/
						tt = cach.get(setNum).removeFirst(); //get cache value for block
						int ret = tt.start;
						int start = 0;
						int end = 2;
						for(int i = ret; i<ret+tt.data.length()/2; i++)
						{
							mem[i] = tt.data.substring(start, end); //put it in memory at block number
							start+=2;
							end+=2;
						}
					}
							
					//load into cache
					/***********get main memory write to cache*************/
					temp = mem[first]; //start address of block
					for(int i = first+1; i<first+blockSize; i++)
						temp+=mem[i];  //start of block with address
					tt = new Block(temp, first); //adds updated info
					if (find*2+bytes*2 < temp.length())
						System.out.println(temp.substring(find*2, find*2+bytes*2));
					else
						System.out.println(temp.substring(find*2));
					tt.dirtyBit = 0;
					cach.get(setNum).addLast(tt); //puts new info at end of cache linked list
				}
			}
		}
		
		
	}
}
