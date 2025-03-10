import java.util.*;

interface KnowledgeSource
{   
    boolean execCondition(BlackboardStore blackboardStore);
    void execAction(BlackboardStore blackboardStore);
}

class BlackboardStore
{
    private List<String> messages=new ArrayList<>();

    public void addMessages(List<String> newMessages)
    {
        messages.addAll(newMessages);
    }

    public List<String> getMessages()
    {
        return new ArrayList<>(messages);
    }

    public void updatedMessage(List<String> updatedMessages)
    {
        messages.clear();
        messages.addAll(updatedMessages);
    }

    public void printMessages(List<String> messages) 
    {
        for(String message: messages)
        {
            System.out.println(message);
        }
    } 

    public boolean isEmpty()
    {
        return messages.isEmpty();
    }
}

class BuyerFilter implements KnowledgeSource
{
    private HashSet<String> buyers;

    public BuyerFilter(HashSet<String> buyers)
    {
        this.buyers=buyers;
    }

    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> validMessages =new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            String[] parts=message.split(", ");
            if(parts.length>=2 && buyers.contains(parts[0]+" - "+parts[1]))
            {
                validMessages.add(message);
            }
        }

        blackboardStore.updatedMessage(validMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class ProfanityFilter implements KnowledgeSource
{
    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> filtredMessages =new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            if(!message.contains("@#$%"))
            {
                filtredMessages.add(message);
            }
        }

        blackboardStore.updatedMessage(filtredMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class PoliticalFilter implements KnowledgeSource
{
    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> filtredMessages =new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            if(!message.contains("---") && !message.contains("+++"))
            {
                filtredMessages.add(message);
            }
        }

        blackboardStore.updatedMessage(filtredMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class ImageResizer implements KnowledgeSource
{
    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> updatedMessages=new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            String[] words=message.split(", ");
            if(words.length==4)
            {
                words[3]=words[3].toLowerCase();
            }

            updatedMessages.add(String.join(", ", words));
        }

        blackboardStore.updatedMessage(updatedMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class LinkRemover implements KnowledgeSource
{
    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> updatedMessages=new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            updatedMessages.add(message.replace("http", ""));    
        }

        blackboardStore.updatedMessage(updatedMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class SentimentAnalyzer implements KnowledgeSource
{
    public void execAction(BlackboardStore blackboardStore)
    {
        List<String> updatedMessages=new ArrayList<>();

        for(String message : blackboardStore.getMessages())
        {
            String[] words=message.split(", ");
            if(words.length>=3)
            {
                String reviewedText=words[2];
                int upperCase=0;
                int lowerCase=0;

                for(char c: reviewedText.toCharArray())
                {
                    if(Character.isUpperCase(c))
                    {
                        upperCase++;
                    }
                    else if(Character.isLowerCase(c))
                    {
                        lowerCase++;
                    }
                }

                if(upperCase>lowerCase)
                {
                    words[2]+="+";
                }
                else if(lowerCase>upperCase)
                {
                    words[2]+="-";
                }
                else
                {
                    words[2]+="=";
                }
            }

            updatedMessages.add(String.join(", ", words));
        }

        blackboardStore.updatedMessage(updatedMessages);
    }

    public boolean execCondition(BlackboardStore blackboardStore)
    {
        return !blackboardStore.isEmpty();
    }
}

class Control
{
    private BlackboardStore blackboardStore;
    private List<KnowledgeSource> knowledgeSources=new ArrayList<>();

    public Control(BlackboardStore blackboardStore)
    {
        this.blackboardStore=blackboardStore;
    }

    public void addKnowledgeSource(KnowledgeSource knowledgeSource)
    {
        knowledgeSources.add(knowledgeSource);
    }

    public void execute()
    {
        boolean changed;
        int iteration=0;

        do
        {
            changed=false;
            System.out.println("\nIteration "+ (++iteration) +" - Before processing "+ blackboardStore.getMessages());
            for(KnowledgeSource knowledgeSource:knowledgeSources)
            {
                if(knowledgeSource.execCondition(blackboardStore))
                {
                    int beforeSize = blackboardStore.getMessages().size();
                    knowledgeSource.execAction(blackboardStore);
                    int afterSize = blackboardStore.getMessages().size();

                    if(beforeSize!=afterSize)
                    {
                        changed=true;
                    }
                }
            }
            System.out.println("Iteration " + iteration + " - After Processing: " + blackboardStore.getMessages());
            System.out.println();

        } while(changed && !blackboardStore.isEmpty());
    }
}

public class Blackboard 
{
    public static void main(String[] args) 
    {
        HashSet<String> buyers=new HashSet<>(Arrays.asList("John - Laptop", "Mary - Phone", "Ann - BigMac"));

        List<String> messages= Arrays.asList(
            "John, Laptop, httpok, PICTURE",
            "Mary, Phone, @#$%), IMAGE",
            "Peter, Phone, GREAT, AloToFpiCtureS",
            "Ann, BigMac, So GOOD, Image"
        );

        BlackboardStore blackboardStore=new BlackboardStore();
        blackboardStore.addMessages(messages);

        Control controller=new Control(blackboardStore);
        controller.addKnowledgeSource(new BuyerFilter(buyers));
        controller.addKnowledgeSource(new ProfanityFilter());
        controller.addKnowledgeSource(new PoliticalFilter());
        controller.addKnowledgeSource(new ImageResizer());
        controller.addKnowledgeSource(new LinkRemover());
        controller.addKnowledgeSource(new SentimentAnalyzer());

        blackboardStore.printMessages(messages);
        System.out.println();
        
        controller.execute();

        blackboardStore.printMessages(messages);
    }   
}
