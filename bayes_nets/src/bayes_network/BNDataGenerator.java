package bayes_network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import pair.Pair;
import bayes_network.cpd.CPDQuery;
import data.DataSet;
import data.attribute.Attribute;
import data.instance.Instance;
import data.instance.InstanceSet;

/**
 * Used for generating a data set from a learned Bayesian Network.
 * 
 * @author Matthew Bernstein - matthewb@cs.wisc.edu
 *
 */
public class BNDataGenerator 
{
    /**
     * This method produces an artificial data set generated from a Bayesian
     * network.
     * 
     * @param net the Bayesian network used to generate the data set
     * @param numInstances the number of instances to be generated
     * @return an artificial data set
     */
    public static DataSet generateDataSet(BayesianNetwork net, int numInstances)
    {
        /*
         * Create data set
         */
        DataSet generated = new DataSet();
        generated.setAttributeSet(net.nodes.getAttributeSet());
        
        /*
         * Build instance set and set the instance set to the new data set
         */
        InstanceSet instances = new InstanceSet();
        
        for (int i = 0; i < numInstances; i++)
        {
            Instance newInst = new Instance();

            for (BNNode node : net.nodes.topologicallySorted())
            {                              
                setAttrInstance(node, newInst); 
                System.out.print("\n");
            }
            
            instances.addInstance(newInst);
        }
        
        generated.setInstanceSet(instances);
        
        return generated;   
    }
    
    
    /**
     * Given a node and an instance under construction, consider the value of
     * the parent attributes that have already been picked for this instance.
     * Choose the value for the specified node's attribute based on the 
     * values of the values of these parent attributes in the instance.
     * 
     * @param node the current node for which we need to pick a value to
     * assign to the instance
     * @param instance the instance for which we need to assign the value of the
     * specified node's attribute.  This instance MUST have values for the 
     * node's parent attributes in the network.  If this is not the case,
     * this method will throw an exception.
     */
    private static void setAttrInstance(BNNode node, Instance instance)
    {
        
        /*
         * The current attribute
         */
        Attribute thisAttr = node.getAttribute();
        
        /*
         * Maps a nominal value ID of the current attribute to a probability
         */
        Map<Double, Double> valueProbabilities = 
                    new HashMap<Double, Double>();
        
        System.out.println("ATTRIBUTE: " + thisAttr.getName());
        
        /*
         * Stores attribute/value pairs
         */
        ArrayList<Pair<Attribute, Integer>> parentValues
                                   = new ArrayList<Pair<Attribute, Integer>>();
        /*
         * Find parent's attribute/value pairs
         */
        for (BNNode parentNode : node.getParents())
        {

            Attribute parentAttr = parentNode.getAttribute();
            Double instValue = instance.getAttributeValue(parentAttr.getId());
            
            System.out.println("PARENT:" + parentNode.getAttribute().getName());
            
            if (instValue == null)
            {
                throw new RuntimeException("Error. While determining new " +
                        "instance value for attribute " + thisAttr.getName() + 
                        " the parent attribute, " + parentAttr.getName() + 
                        " value for this instance is null.");
            }
            
            Pair<Attribute, Integer> valuePair 
                        = new Pair<Attribute, Integer>(parentAttr, instValue.intValue());
            parentValues.add(valuePair);
        }
    
        for (Integer nominalValue : node.getAttribute().getNominalValueMap().values())
        {
            CPDQuery query = new CPDQuery();
            for (Pair<Attribute, Integer> value : parentValues)
            {
                query.addQueryItem(value.getFirst(), value.getSecond());
            }
            query.addQueryItem(node.getAttribute(), nominalValue);
            
            valueProbabilities.put(nominalValue.doubleValue(), 
                                   node.query(query) );
        }
        
        
        Double value = pickRandomValue(valueProbabilities);
        System.out.println("PICKED " + thisAttr.getNominalValueName(value.intValue()));
        
        instance.addAttributeInstance(thisAttr.getId(), value);
        
        //TODO REMOVE THIS!
        System.out.println("PROBABILITIES" + thisAttr.getName());
        for (Entry<Double, Double> entry : valueProbabilities.entrySet())
        {   
            String valName = thisAttr.getNominalValueName(entry.getKey().intValue());
            System.out.println(valName + " : " + entry.getValue());
        }
    }
    
    public static Double pickRandomValue(Map<Double, Double> valueProbabilities)
    {
        Random rand = new Random();
        double pick = rand.nextDouble();
        
        /*
         * Begin range/end range. If the value of the pick falls in this range,
         * return the current value.  The size of this range is proportional
         * to the probability of picking that value.
         */
        double beginRange = 0;
        double endRange = 0;
       
        
        for (Entry<Double, Double> entry : valueProbabilities.entrySet())
        {
           // Update ranges
           beginRange = endRange;
           endRange += entry.getValue();
           
           // Determine if this value has been chosen
           if (pick >= beginRange && pick < endRange)
           {               
               return entry.getKey();
           }
        }
        
        return null;
    }
    

}