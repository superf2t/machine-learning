package tree.algorithms;

import java.util.ArrayList;
import java.util.List;

import classify.Classifier;
import tree.DecisionTree;
import tree.DtLeaf;
import tree.DtNode;
import tree.Node;
import tree.DtNode.Relation;
import tree.train.Split;
import tree.train.SplitBranch;
import tree.train.SplitGenerator;
import data.Attribute;
import data.DataSet;

public abstract class DecisionTreeBuilder 
{

	/**
	 * The decision tree under construction
	 */
	protected DecisionTree decisionTree = null;

	/**
	 * Determine when the recursion should stop and a leaf node should be constructed.
	 * 
	 * @param data the data set used to make this decision
	 * @param availAttributes available attributes 
	 * @param candidateSplits splits that are under consideration to split on
	 * @return whether or not the recursion should terminate
	 */
	protected abstract boolean checkStoppingCriteria(DataSet data,
			List<Attribute> availAttributes,
			List<Split> candidateSplits);
	
	/**
	 * Determine the best attribute to split on.
	 * 
	 * @param data the data set used to make this decision
	 * @param candidateSplits the set of candidate split
	 * @return the best split among the candidates
	 */
	protected abstract Split determineBestSplit(DataSet data, List<Split> candidateSplits);

	public DecisionTree buildDecisionTree(DataSet data)
	{
		List<Attribute> availableAttributes = removeAttributeById(data.getAttributeSet().getAttributes(),
				data.getClassAttributeId());

		DtNode root = makeSubTree( 
				data, 
				null,
				null,
				null,
				availableAttributes);
		
		decisionTree = new DecisionTree(root, data.getClassAttribute());
		return decisionTree;	
	}

	private DtNode makeSubTree(
			DataSet data,
			Attribute attribute,
			Double value,
			DtNode.Relation relation,
			List<Attribute> availAttrs)
	{		
		DtNode newNode = null;
		List<Split> candidateSplits = SplitGenerator.generateSplits(data, availAttrs);

		/*
		 *  If the stopping criteria is met, create a leaf node with a decision 
		 *  class label that is the majority class of the instances at this node
		 */ 
		if (checkStoppingCriteria(data, availAttrs, candidateSplits))
		{
			DtLeaf leaf = new DtLeaf(attribute, 
					value, 
					relation, 
					data.getMajorityClass());
			leaf.setClassCounts(data.getClassCounts());
			newNode = leaf;
		}
		else
		{
			newNode = new DtNode(attribute, value, relation);
			newNode.setClassCounts(data.getClassCounts());

			/*
			 *   Find the best split among the candidate splits. For each branch of the best split, create a 
			 *   new node that roots a subtree
			 */
			Split bestSplit = determineBestSplit(data, candidateSplits);
			for (SplitBranch branch : bestSplit.getSplitBranches())
			{	
				DataSet subsetData = new DataSet();
				subsetData.setAttributeSet(data.getAttributeSet());
				subsetData.setInstanceSet(branch.getInstanceSet());
				subsetData.setClassAttribute(data.getClassAttribute().getName());

				/*
				 *  Determine attributes that are still available after the split.  We only remove 
				 *  the attribute if it is nominal.  
				 */
				List<Attribute> newAvailableAttributes;
				if (branch.getAttribute().getType() == Attribute.Type.NOMINAL)
				{
					newAvailableAttributes = removeAttributeById(availAttrs,
							bestSplit.getAttribute().getId());
				}
				else
				{
					newAvailableAttributes = availAttrs;
				}

				/*
				 *  Make the recursive call to make a subtree at each child node
				 */
				Node child = makeSubTree(
						subsetData,
						bestSplit.getAttribute(),
						branch.getValue(),
						branch.getRelation(),
						newAvailableAttributes);

				newNode.addChild(child);
			}
		}
		
		return newNode;
	}
	
	/**
	 * A helper method for removing an attribute with a specific attribute ID
	 * from an ArrayList of Attributes
	 *  
	 * @param currAttrs ArrayList of Attributes from which we wish to 
	 * remove an Attribute
	 * @param attrId the ID of the Attribute we wish to remove
	 * @return
	 */
	private List<Attribute> removeAttributeById(List<Attribute> currAttrs, 
	                                                 Integer attrId)
	{
		List<Attribute> newAttributes = new ArrayList<>();
		
		for (Attribute attr : currAttrs)
		{
			if (attr.getId() != attrId)
			{
				newAttributes.add(attr);
			}
		}
		
		return newAttributes;
	}
}
