package it.unipi.di.acube.smaph.snippetannotationfilters;

import it.unipi.di.acube.batframework.data.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FrequencyAnnotationFilter implements SnippetAnnotationFilter {
	private double threshold;

	public FrequencyAnnotationFilter(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public HashSet<Tag> filterAnnotations(
			HashMap<Tag, List<Integer>> tagToRanks, double resCount) {
		HashSet<Tag> filteredTags = new HashSet<>();
		for (Tag t : tagToRanks.keySet())
			if (((double) tagToRanks.get(t).size()) / (double) resCount >= threshold)
				filteredTags.add(t);
		return filteredTags;
	}

}
