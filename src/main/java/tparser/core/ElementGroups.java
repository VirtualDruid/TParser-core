package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * model class represent ordered sets of elements
 * a single group has types each with only a element of that type,which classified by evaluator
 * <p>
 * its properties can be retrieve by index on each evaluator(type) key
 * <p>
 * a group is also as an item in an array for further extraction if it's under an array
 * every element under a group is nullable by default, unless it has "x-required" attr
 *
 * <p>
 * given group type: {p / div / li}
 * one 'p', one 'li' and one 'div' will be a group
 * <p>
 * [p div div li] will be grouped as [{p null null},{null div null},{null div li}]
 * <p>
 * some example:
 * [p, div, li, div, li] -> [{p div li}, {null div li}]
 * [p, div, p, p, div, p] -> [{p div null}, {p null null}, {p div null}, {p null null}]
 */
class ElementGroups {

    private int groupsFound         = 0;
    //the current sub array size
    private int currentSubArraySize = 0;

    //evaluators as keys of element's type
    private IdentityHashMap<Evaluator, ArrayList<Element>> classified;

    //the sub array sizes used by ArrayVisitor
    //classified elements will be spilt by sizes
    private ArrayList<Integer> sizes = new ArrayList<>();

    ElementGroups(List<Evaluator> classifications) {
        classified = new IdentityHashMap<>(CollectionHelper.enoughHashTableCapacity(classifications.size()));
        for (Evaluator evaluator : classifications) {
            classified.put(evaluator, new ArrayList<>());
        }
    }

    private void fillPreviousGroupsWithNull(Element parent) {
        //first group will never execute the loop (empty entries)
        for (Map.Entry<Evaluator, ArrayList<Element>> entry : classified.entrySet()) {
            //fill the group by adding missing elements as null
            while (entry.getValue().size() < groupsFound) {
                entry.getValue().add(NullWrapper.nullRepresent(parent));
            }
        }
    }

    /**
     * all the parents are processed
     * finish the last group
     */
    void onParentsAllVisited(Elements parents) {
        Element lastParent = parents.last();
        fillPreviousGroupsWithNull(lastParent);
    }

    /**
     * found first type of element in a group
     * create a new group and finish the previous
     *
     * @see Classifier
     */
    void onShouldNewGroup(Element parent, Element element, Evaluator eval) {
//        //first group will never execute the loop (empty entries)
//        for (Map.Entry<Evaluator, ArrayList<Element>> entry : classified.entrySet()) {
//            List<Element> classifiedElements = entry.getValue();
//            //fill the group by adding missing elements as null
//            if (classifiedElements.size() < groupsFound) {
//                classifiedElements.add(NullWrapper.nullRepresent(parent));
//            }
//        }
        fillPreviousGroupsWithNull(parent);
        classified.get(eval).add(element);
        groupsFound++;
        currentSubArraySize++;
    }

    /**
     * on found non-first type element in a group
     */
    void onFound(Element element, Evaluator eval) {
        classified.get(eval).add(element);
    }

    /**
     * start at a new root
     * reset sub array size
     */
    void onStartOfParent() {
        currentSubArraySize = 0;
    }

    /**
     * end of a root
     * the sub array is complete
     */
    void onEndOfParent() {
        sizes.add(currentSubArraySize);
    }


    /**
     * get elements of a type inside each group
     *
     * @param classificationKey the evaluator represent the type
     * @return properties of the same type, in those' owner groups order
     * @see ElementVisitor
     */
    ArrayList<Element> getClassifiedElements(Evaluator classificationKey) {
        return classified.get(classificationKey);
    }

    /**
     * get sub array sizes
     *
     * @see ArrayVisitor
     */
    List<Integer> getSizes() {
        return sizes;
    }

    /**
     * get how many groups found
     *
     * @see ArrayVisitor
     * @see ObjectVisitor
     */
    int getGroupsFound() {
        return groupsFound;
    }

}
