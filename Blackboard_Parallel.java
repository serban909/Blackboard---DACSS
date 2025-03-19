import java.io.*;
import java.util.*;
import java.util.concurrent.*;

interface KnowledgeSource extends Runnable
{
    public boolean isEliminator();
    public boolean execCondition(BlockingQueue<String> sharedQueue);
}

class ParallelBlackboard 
{
    private BlockingQueue<String> sharedQueue = new LinkedBlockingQueue<>();
    private int eliminatorCounter = 0;
    private boolean transformationFlag = false;
    private boolean updatedMessage;

    public BlockingQueue<String> getQueue()
    {
        return sharedQueue;
    }

    public synchronized void increaseEliminatorCounter() 
    {
        eliminatorCounter++;
    }

    public synchronized boolean areEliminatorFinished(int eliminatorCount) 
    {
        return eliminatorCounter >= eliminatorCount;
    }

    public synchronized void setTransformationFlag(boolean value) 
    {
        transformationFlag = value;
    }

    public synchronized boolean canTransform() 
    {
        return transformationFlag;
    }

    public synchronized int getEliminatorCounter()
    {
        return eliminatorCounter;
    }

    public synchronized void setUpdatedMessage(boolean value)
    {
        updatedMessage=value;
    }

    public synchronized boolean isUpdatedMessage()
    {
        return updatedMessage;
    }

    public synchronized void resetMessage()
    {
        updatedMessage=false;
    }
}

class Control
{
    private ParallelBlackboard blackboard = new ParallelBlackboard();
    private final List<KnowledgeSource> eliminators = new ArrayList<>();
    private final List<KnowledgeSource> transformers = new ArrayList<>();

    
    public Control(ParallelBlackboard blackboard)
    {
        this.blackboard = blackboard;
    }

    public void addKnowledgeSource(KnowledgeSource filter)
    {
        if (filter.isEliminator())
        {
            eliminators.add(filter);
        }
        else
        {
            transformers.add(filter);
        }
    }

    public void execute()
    {
        List<Thread> threads = new ArrayList<>();

        for( KnowledgeSource filter : eliminators )
        {
            if(filter.execCondition(blackboard.getQueue()))
            {
                Thread thread = new Thread(filter);
                thread.start();
                threads.add(thread);
            }
        }

        try 
        {
            Thread.sleep(200);
        } 
        catch (InterruptedException e) 
        {
            Thread.currentThread().interrupt();
        }

        blackboard.resetMessage();

        if(blackboard.areEliminatorFinished(eliminators.size()))
        {
            for( KnowledgeSource filter : transformers )
            {
                if(filter.execCondition(blackboard.getQueue()))
                {
                    Thread thread = new Thread(filter);
                    thread.start();
                    threads.add(thread);
                }
            }
        }

        for(Thread thread : threads)
        {
            try 
            {
                thread.join();
            }
            catch(InterruptedException e) 
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ReaderFilter implements KnowledgeSource
{
    private String inputFile;
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public ReaderFilter(String inputFile, BlockingQueue<String> bQueue, ParallelBlackboard blackboard) 
    {
        this.inputFile = inputFile;
        this.bQueue = bQueue;
        this.blackboard= blackboard;
    }

    public void run()
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) 
        {
            String line;
            while ((line = reader.readLine()) != null) 
            {
                bQueue.put(line);
            }
            blackboard.increaseEliminatorCounter();
            bQueue.put("STOP");
        }
        catch(IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isEliminator()
    {
        return true;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return true;
    }
}

class BuyerFilter implements KnowledgeSource
{
    private HashSet<String> buyers;
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public BuyerFilter(HashSet<String> buyers, BlockingQueue<String> bQueue, ParallelBlackboard blackboard) 
    {
        this.buyers = buyers;
        this.bQueue=bQueue;
        this. blackboard = blackboard;
    }

    public void run()
    {
        try
        {
            while(true)
            {
                String message=bQueue.take();
                if(message.equals("STOP")) break;

                String[] words = message.split(", ");
                if(words.length >= 2 && buyers.contains(words[0].trim()+" - "+words[1].trim()))
                {
                    bQueue.put(message);
                }
            }
            blackboard.setUpdatedMessage(true);
            blackboard.increaseEliminatorCounter();
            bQueue.put("STOP");
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return true;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return blackboard.isUpdatedMessage()==false;
    }
}

class ProfanityFilter implements KnowledgeSource 
{
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public ProfanityFilter(BlockingQueue<String> bQueue, ParallelBlackboard blackboard)
    {
        this.bQueue=bQueue;
        this.blackboard =blackboard;
    }

    public void run()
    {
        try
        {
            while(true)
            {
                String message=bQueue.take();
                if(message.equals("STOP")) break;

                if(!message.contains("@#$%"))
                {
                    bQueue.put(message);
                }
            }
            blackboard.setUpdatedMessage(true);
            blackboard.increaseEliminatorCounter();
            bQueue.put("STOP");
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return true;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return !blackboard.isUpdatedMessage();
    }
}

class PoliticalFilter implements KnowledgeSource
{
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public PoliticalFilter (BlockingQueue<String> bQueue, ParallelBlackboard blackboard)
    {
        this.bQueue =bQueue;
        this.blackboard = blackboard;
    }

    public void run()
    {
        try
        {
            while(true)
            {
                String message=bQueue.take();
                if(message.equals("STOP")) break;

                if(!message.contains("+++") && !message.contains("---"))
                {
                    bQueue.put(message);
                }
            }
            blackboard.setUpdatedMessage(true);
            blackboard.increaseEliminatorCounter();
            bQueue.put("STOP");
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return true;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return !blackboard.isUpdatedMessage();
    }
}

class ImageResizer implements KnowledgeSource
{
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public ImageResizer (BlockingQueue<String> bQueue, ParallelBlackboard blackboard)
    {
        this.bQueue =bQueue;
        this.blackboard = blackboard;
    }

    public void run()
    {
        blackboard.setTransformationFlag(true);
        try
        {
            while(true)
            {
                String message=bQueue.take();
                if(message.equals("STOP")) break;

                String[] parts=message.split(", ");
                if(parts.length>=4)
                {
                    parts[3]=parts[3].toLowerCase();
                }
                bQueue.put(String.join(", ", parts));
            }
            blackboard.setTransformationFlag(true);
            bQueue.put("STOP");
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return false;
    }


    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return !blackboard.isUpdatedMessage();
    }
}

class LinkRemover implements KnowledgeSource
{
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public LinkRemover (BlockingQueue<String> bQueue, ParallelBlackboard blackboard)
    {
        this.bQueue =bQueue;
        this. blackboard = blackboard;
    }

    public void run()
    {
        blackboard.setTransformationFlag(true);
        try
        {
            while(true)
            {
                String message =bQueue.take();
                if(message.equals("STOP")) break;

                bQueue.put(message.replace("http", ""));
            }
            blackboard.setUpdatedMessage(true);
            bQueue.put("STOP");
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return false;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return !blackboard.isUpdatedMessage();
    }
}

class SentimentAnalyzer implements KnowledgeSource
{
    private BlockingQueue<String> bQueue;
    private ParallelBlackboard blackboard;

    public SentimentAnalyzer (BlockingQueue<String> bQueue, ParallelBlackboard blackboard)
    {
        this.bQueue =bQueue;
        this.blackboard = blackboard;
    }

    public void run()
    {
        blackboard.setTransformationFlag(true);
        try
        {
            while(true)
            {
                String message =bQueue.take();
                if(message.equals("STOP")) break;
                
                String[] parts = message.split(", ");
                if (parts.length >= 3 && !parts[2].isEmpty()) 
                {
                    String reviewedText = parts[2];
                    int upper = 0, lower = 0;

                    for (char c : reviewedText.toCharArray()) 
                    {
                        if (Character.isUpperCase(c)) upper++;
                        else if (Character.isLowerCase(c)) lower++;
                    }

                    if (upper > lower) 
                    {
                        parts[2] += "+";
                    } 
                    else if (lower > upper) 
                    {
                        parts[2] += "-";
                    } 
                    else 
                    {
                        parts[2] += "=";
                    }
                }

                bQueue.put(String.join(", ", parts));
            }
            blackboard.setUpdatedMessage(true);
            bQueue.put("STOP");
        }
        catch (InterruptedException e) 
        {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEliminator()
    {
        return false;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return !blackboard.isUpdatedMessage();
    }
}

class WriterFilter implements KnowledgeSource
{
    private BlockingQueue<String> bQueue;
    private String outputFile;

    public WriterFilter(BlockingQueue<String> bQueue, String outputFile)
    {
        this.bQueue=bQueue;
        this.outputFile=outputFile;
    }

    public void run()
    {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
        {
            while(true)
            {
                String message=bQueue.take();
                if(message.equals("STOP")) break;

                writer.write(message);
                writer.newLine();
            }
            bQueue.put("STOP");
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isEliminator()
    {
        return false;
    }

    public boolean execCondition(BlockingQueue<String> sharedQueue)
    {
        return true;
    }
}

public class Blackboard_Parallel 
{
    public static void main(String[] args)
    {
        String inputFile = "input.txt";
        String outputFile = "output2.txt";

        ParallelBlackboard blackboard = new ParallelBlackboard();
        BlockingQueue<String> sharedQueue = blackboard.getQueue();

        Control control = new Control(blackboard);

        long startTime = System.currentTimeMillis();

        ReaderFilter readerFilter = new ReaderFilter(inputFile, sharedQueue, blackboard);

        readerFilter.run();

        HashSet<String> buyers=new HashSet<>(Arrays.asList
        (
            "John - Laptop", 
            "Mary - Phone",
            "Ann - BigMac",
            "Emanuel - Hyundai",
            "Razvan - Jas39",
            "Bob - Notebook",
            "Bogdan - Parrot",
            "Radu - Dog",
            "Lucian - Shoes",
            "Mihai - Coke",
            "Calin - Pants",
            "Stefan - Pen",
            "Toni - Guitar",
            "Luca - Football",
            "Andrei - Car",
            "Flavius - Shirt",
            "Marian - Outlet",
            "Peter - Tractor",
            "Piedone - Shawarma",
            "Matei - DVD",
            "Vasile - Wine",
            "Marioara - Cupcake",
            "Ghita - Toolbox",
            "Miriam - Mask",
            "Alex - MacBook",
            "Nicu - Sandwich",
            "Laura - Fish",
            "Sebastian - Flower",
            "Daniel - Bonsai",
            "Terry - Silver"
        ));

        control.addKnowledgeSource(new ProfanityFilter(sharedQueue, blackboard));
        control.addKnowledgeSource(new PoliticalFilter(sharedQueue, blackboard));
        control.addKnowledgeSource(new BuyerFilter(buyers, sharedQueue, blackboard));

        blackboard.setTransformationFlag(true);

        control.addKnowledgeSource(new SentimentAnalyzer(sharedQueue, blackboard));
        control.addKnowledgeSource(new ImageResizer(sharedQueue, blackboard));
        control.addKnowledgeSource(new LinkRemover(sharedQueue, blackboard));

        WriterFilter writerFilter = new WriterFilter(sharedQueue, outputFile);

        control.execute();

        writerFilter.run();

        long endTime = System.currentTimeMillis();

        System.out.println("Blackboard Execution Time: "+(endTime-startTime)+" ms");
    }    
}
