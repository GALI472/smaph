package it.acubelab.smaph.linkback;

import it.acubelab.batframework.data.Annotation;
import it.acubelab.batframework.data.ScoredAnnotation;
import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.utils.Pair;
import it.acubelab.batframework.utils.WikipediaApiInterface;
import it.acubelab.smaph.QueryInformation;
import it.acubelab.smaph.SmaphUtils;
import it.acubelab.smaph.learn.featurePacks.AnnotationFeaturePack;
import it.acubelab.smaph.learn.normalizer.FeatureNormalizer;
import it.acubelab.smaph.linkback.annotationRegressor.Regressor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.tartarus.snowball.ext.EnglishStemmer;

public class SvmIndividualAnnotationLinkBack implements LinkBack {
	private Regressor ar;
	private FeatureNormalizer annFn;
	private WikipediaApiInterface wikiApi;
	private double threshold;

	public SvmIndividualAnnotationLinkBack(Regressor ar,
			FeatureNormalizer annFn, WikipediaApiInterface wikiApi,
			double threshold) {
		this.ar = ar;
		this.annFn = annFn;
		this.wikiApi = wikiApi;
		this.threshold = threshold;
	}

	public static List<Annotation> getAnnotations(String query,
			Set<Tag> acceptedEntities, QueryInformation qi){
		List<Pair<Integer, Integer>> segments = SmaphUtils.findSegments(query);
		List<Annotation> annotations = new Vector<>();
		for (Tag t : acceptedEntities)
			for (Pair<Integer, Integer> segment : segments) 
				annotations.add(new Annotation(segment.first, segment.second-segment.first, t.getConcept()));
				
		return annotations;
	}
	
	@Override
	public HashSet<ScoredAnnotation> linkBack(String query,
			HashSet<Tag> acceptedEntities, QueryInformation qi) {

		HashMap<Tag, String> entityToTitle = SmaphUtils.getEntitiesToTitles(
				acceptedEntities, wikiApi);
		HashMap<Tag, String[]> entityToBolds = null;
		if (qi.boldToEntityS1 != null)
			entityToBolds = SmaphUtils.getEntitiesToBolds(qi.boldToEntityS1,
					acceptedEntities);
		else
			entityToBolds = SmaphUtils.getEntitiesToBoldsList(qi.tagToBoldsS6,
					acceptedEntities);

		
		EnglishStemmer stemmer = new EnglishStemmer();

		List<Pair<Annotation, Double>> scoreAndAnnotations = new Vector<>();
		for (Annotation a : getAnnotations(query, acceptedEntities, qi)) {
			double bestScore = Double.NEGATIVE_INFINITY;
			for (HashMap<String, Double> entityFeatures : qi.entityToFtrVects
					.get(new Tag(a.getConcept()))) {
				double score = ar.predictScore(new AnnotationFeaturePack(a, query, stemmer,
						entityFeatures, entityToBolds, entityToTitle), annFn);
				if (score > bestScore)
					bestScore = score;
			}
			scoreAndAnnotations.add(new Pair<Annotation, Double>(a, bestScore));
		}

		return getResult(scoreAndAnnotations, threshold);
	}
	
	public static HashSet<ScoredAnnotation> getResult(List<Pair<Annotation, Double>> annotationsAndScore, double threshold){
		Collections.sort(annotationsAndScore, new SmaphUtils.ComparePairsBySecondElement());
		Collections.reverse(annotationsAndScore);

		HashSet<ScoredAnnotation> res = new HashSet<>();
		
		for (Pair<Annotation, Double> pair : annotationsAndScore){
			Annotation annI = pair.first;
			double score = pair.second;
			
			if (score < threshold) break;
			
			boolean overlap = false;
			for (ScoredAnnotation ann : res)
				if (annI.overlaps(ann)) {
					overlap = true;
					break;
				}
			if (!overlap){
				res.add(new ScoredAnnotation(annI.getPosition(), annI.getLength(), annI.getConcept(), (float)score));
			}
		}

		//TODO: shall we collapse bindings?
		//return SmaphUtils.collapseBinding(res);
		return res;
	}

}
