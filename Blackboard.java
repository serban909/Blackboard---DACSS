import java.util.*;

interface KnowledgeSource 
{   
    public boolean execCondition(BlackboardStore blackboardStore);
    public void execAction(BlackboardStore blackboardStore);
    public boolean isEliminator();
}

class BlackboardStore 
{
    private List<String> messages = new ArrayList<>();
    private int eliminatorCounter = 0;
    private boolean transformationFlag = false;

    public void addMessages(List<String> newMessages) 
    {
        messages.addAll(newMessages);
    }

    public List<String> getMessages() 
    {
        return new ArrayList<>(messages);
    }

    public void updateMessages(List<String> updatedMessages) 
    {
        messages.clear();
        messages.addAll(updatedMessages);
    }

    public void printMessages() 
    {
        for (String message : messages) 
        {
            System.out.println(message);
        }
    }

    public boolean isEmpty() 
    {
        return messages.isEmpty();
    }

    public void increaseEliminatorCounter() 
    {
        eliminatorCounter++;
    }

    public boolean areEliminatorFinished(int eliminatorCount) 
    {
        return eliminatorCounter >= eliminatorCount;
    }

    public void setTransformationFlag(boolean value) 
    {
        transformationFlag = value;
    }

    public boolean canTransform() 
    {
        return transformationFlag;
    }
}

class BuyerFilter implements KnowledgeSource 
{
    private Set<String> buyers;

    public BuyerFilter(Set<String> buyers) 
    {
        this.buyers = buyers;
    }

    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> validMessages = new ArrayList<>();

        for (String message : blackboardStore.getMessages()) 
        {
            String[] parts = message.split(", ");
            if (parts.length >= 2 && buyers.contains(parts[0] + " - " + parts[1])) 
            {
                validMessages.add(message);
            }
        }

        blackboardStore.updateMessages(validMessages);
        blackboardStore.increaseEliminatorCounter();
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return true;
    }
}

class ProfanityFilter implements KnowledgeSource 
{
    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> filteredMessages = new ArrayList<>();

        for (String message : blackboardStore.getMessages()) 
        {
            if (!message.contains("@#$%")) 
            {
                filteredMessages.add(message);
            }
        }

        blackboardStore.updateMessages(filteredMessages);
        blackboardStore.increaseEliminatorCounter();
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return true;
    }
}

class PoliticalFilter implements KnowledgeSource 
{
    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> filteredMessages = new ArrayList<>();

        for (String message : blackboardStore.getMessages()) 
        {
            if (!message.contains("---") && !message.contains("+++")) 
            {
                filteredMessages.add(message);
            }
        }

        blackboardStore.updateMessages(filteredMessages);
        blackboardStore.increaseEliminatorCounter();
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return true;
    }
}

class ImageResizer implements KnowledgeSource 
{
    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> updatedMessages = new ArrayList<>();
        boolean modified = false;

        for (String message : blackboardStore.getMessages()) 
        {
            String[] words = message.split(", ");
            if (words.length == 4) 
            {
                words[3] = words[3].toLowerCase();
                modified = true;
            }
            updatedMessages.add(String.join(", ", words));
        }

        blackboardStore.updateMessages(updatedMessages);
        blackboardStore.setTransformationFlag(modified);
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return blackboardStore.areEliminatorFinished(3) && !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return false;
    }
}

class LinkRemover implements KnowledgeSource 
{
    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> updatedMessages = new ArrayList<>();
        boolean modified = false;

        for (String message : blackboardStore.getMessages()) 
        {
            String newMessage = message.replace("http", "");
            if (!newMessage.equals(message)) 
            {
                modified = true;
            }
            updatedMessages.add(newMessage);
        }

        blackboardStore.updateMessages(updatedMessages);
        blackboardStore.setTransformationFlag(modified);
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return blackboardStore.areEliminatorFinished(3) && !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return false;
    }
}

class SentimentAnalyzer implements KnowledgeSource 
{
    public void execAction(BlackboardStore blackboardStore) 
    {
        List<String> updatedMessages = new ArrayList<>();
        boolean modified = false;

        for (String message : blackboardStore.getMessages()) 
        {
            String[] words = message.split(", ");
            if (words.length >= 3) 
            {
                String review = words[2];
                int upper=0;
                int lower=0;

                for(char c:review.toCharArray())
                {
                    if (Character.isUpperCase(c))
                    {
                        upper++;
                    }
                    else if (Character.isLowerCase(c))
                    {
                        lower++;
                    }
                }

                if(upper>lower)
                {
                    words[2]+="+";
                }
                else if(lower>upper)
                {
                    words[2]+="-";
                }
                else
                {
                    words[2]+="=";
                }

                modified = true;
            }
            updatedMessages.add(String.join(", ", words));
        }

        blackboardStore.updateMessages(updatedMessages);
        blackboardStore.setTransformationFlag(modified);
    }

    public boolean execCondition(BlackboardStore blackboardStore) 
    {
        return blackboardStore.areEliminatorFinished(3) && !blackboardStore.isEmpty();
    }

    public boolean isEliminator() 
    {
        return false;
    }
}

class Control {
    private final BlackboardStore blackboardStore;
    private final List<KnowledgeSource> eliminators = new ArrayList<>();
    private final List<KnowledgeSource> transformers = new ArrayList<>();

    public Control(BlackboardStore blackboardStore) 
    {
        this.blackboardStore = blackboardStore;
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
        Collections.shuffle(eliminators);
        System.out.println("Elimiators order: "+eliminators);
        for (KnowledgeSource filter : eliminators) 
        {
            if (filter.execCondition(blackboardStore)) 
            {
                filter.execAction(blackboardStore);
            }
        }

        Collections.shuffle(transformers);
        System.out.println("Transformers order: "+transformers);
        for (KnowledgeSource filter : transformers) 
        {
            if (filter.execCondition(blackboardStore) && blackboardStore.canTransform()) 
            {
                filter.execAction(blackboardStore);
            }
        }
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

        blackboardStore.printMessages();
        System.out.println();
        
        controller.execute();

        blackboardStore.printMessages();
    }   
}
