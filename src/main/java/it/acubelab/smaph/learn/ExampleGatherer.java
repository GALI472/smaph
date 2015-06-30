/**
 *  Copyright 2014 Marco Cornolti
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.acubelab.smaph.learn;

import it.acubelab.batframework.utils.Pair;
import it.acubelab.smaph.SmaphUtils;
import it.acubelab.smaph.learn.featurePacks.FeaturePack;
import it.acubelab.smaph.learn.normalizer.FeatureNormalizer;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import libsvm.*;

public class ExampleGatherer<T extends Serializable, G extends Serializable> {
	private List<List<Pair<FeaturePack<T>, Double>>> featureVectorsAndTargetGroups = new Vector<>();
	private List<G> groupGolds = new Vector<>();
	private List<List<T>> groupDatas = new Vector<>();
	private int ftrCount = -1;

	/**
	 * Add all examples of an instance, forming a new group.
	 * 
	 * @param list
	 *            normalized feature vectors of this group, plus their gold
	 *            value.
	 */
	public void addExample(List<Pair<FeaturePack<T>, Double>> list) {
		addExample(list, null, null);
	}

	/**
	 * Add all examples of an instance, forming a new group.
	 * 
	 * @param list
	 *            normalized feature vectors of this group, plus their gold
	 *            value.
	 * @param groupData
	 *            for each element of the list, the original data that produced
	 *            that element.
	 * 
	 * @param gold
	 *            the gold standard for this instance.
	 */
	public void addExample(List<Pair<FeaturePack<T>, Double>> list,
			List<T> groupData, G gold) {
		if (groupData != null && list.size() != groupData.size())
			throw new RuntimeException(
					"groupData must contain an element for each feature pack.");
		groupDatas.add(groupData);
		groupGolds.add(gold);
		featureVectorsAndTargetGroups.add(list);
		if (ftrCount < 0)
			for (Pair<FeaturePack<T>, Double> p : list)
				ftrCount = p.first.getFeatureCount();
	}

	/**
	 * @return the number of examples.
	 */
	public int getExamplesCount() {
		int count = 0;
		for (List<Pair<FeaturePack<T>, Double>> featureVectorAndGold : featureVectorsAndTargetGroups)
			count += featureVectorAndGold.size();
		return count;
	}

	public List<FeaturePack<T>> getAllFeaturePacks() {
		List<FeaturePack<T>> ftrPacks = new Vector<FeaturePack<T>>();
		for (List<Pair<FeaturePack<T>, Double>> featureVectorsAndGoldGroup : featureVectorsAndTargetGroups)
			for (Pair<FeaturePack<T>, Double> featureVectorsAndGold : featureVectorsAndGoldGroup)
				ftrPacks.add(featureVectorsAndGold.first);
		return ftrPacks;
	}

	private List<Pair<FeaturePack<T>, Double>> getPlain() {
		Vector<Pair<FeaturePack<T>, Double>> res = new Vector<>();
		for (List<Pair<FeaturePack<T>, Double>> group : featureVectorsAndTargetGroups)
			for (Pair<FeaturePack<T>, Double> ftrVectAndGold : group) {
				res.add(new Pair<FeaturePack<T>, Double>(ftrVectAndGold.first,
						ftrVectAndGold.second));
			}
		return res;
	}

	/**
	 * @return the number of features of the gathered examples.
	 */
	public int getFtrCount() {
		return ftrCount;
	}

	/**
	 * @return a libsvm problem (that is, a list of examples) including all
	 *         features.
	 */
	public svm_problem generateLibSvmProblem(FeatureNormalizer fn) {
		return generateLibSvmProblem(
				SmaphUtils.getAllFtrVect(this.getFtrCount()), fn);
	}

	/**
	 * @param pickedFtrs
	 *            the list of features to pick.
	 * @return a libsvm problem (that is, a list of examples) including only
	 *         features given in pickedFtrs.
	 */
	public svm_problem generateLibSvmProblem(int[] pickedFtrs,
			FeatureNormalizer fn) {
		Vector<Double> targets = new Vector<Double>();
		Vector<svm_node[]> ftrVectors = new Vector<svm_node[]>();
		List<Pair<FeaturePack<T>, Double>> plainVectors = getPlain();
		for (Pair<FeaturePack<T>, Double> vectAndGold : plainVectors) {
			ftrVectors.add(LibSvmModel.featuresArrayToNode(
					fn.ftrToNormalizedFtrArray(vectAndGold.first), pickedFtrs));
			targets.add(vectAndGold.second);
		}
		return createProblem(targets, ftrVectors);
	}

	/**
	 * @param pickedFtrsI
	 *            the list of features to pick.
	 * @return a list of libsvm problems, one per instance.
	 */
	public List<svm_problem> generateLibSvmProblemOnePerInstance(
			int[] pickedFtrs, FeatureNormalizer fn) {

		Vector<svm_problem> result = new Vector<>();

		for (List<Pair<FeaturePack<T>, Double>> ftrVectorsAndGolds : featureVectorsAndTargetGroups) {
			Vector<Double> targets = new Vector<Double>();
			Vector<svm_node[]> ftrVectors = new Vector<svm_node[]>();
			for (Pair<FeaturePack<T>, Double> vectAndGold : ftrVectorsAndGolds) {
				ftrVectors.add(LibSvmModel.featuresArrayToNode(
						fn.ftrToNormalizedFtrArray(vectAndGold.first),
						pickedFtrs));
				targets.add(vectAndGold.second);
			}
			result.add(createProblem(targets, ftrVectors));
		}
		return result;
	}
	
	public List<G> getGold(){
		return groupGolds;
	}

	public List<Pair<Vector<Pair<svm_node[], T>>, G>> getDataAndNodesAndGoldOnePerInstance(
			int[] pickedFtrs, FeatureNormalizer fn){
		
		List<Pair<Vector<Pair<svm_node[], T>>, G>> res = new Vector<>();
		for (int i=0; i<featureVectorsAndTargetGroups.size(); i++){
			List<Pair<FeaturePack<T>, Double>> ftrPackAndTargets = featureVectorsAndTargetGroups.get(i);
			G gold = groupGolds.get(i);
			List<T> data = groupDatas.get(i);
			Vector<Pair<svm_node[], T>> ftrVectors = new Vector<Pair<svm_node[], T>>();
			for (int j=0; j<ftrPackAndTargets.size(); j++){
				Pair<FeaturePack<T>, Double> ftrPackAndTarget = ftrPackAndTargets.get(j);
				ftrVectors.add(new Pair<svm_node[], T>(
						LibSvmModel.featuresArrayToNode(
						fn.ftrToNormalizedFtrArray(ftrPackAndTarget.first),
						pickedFtrs),
						data.get(j)
						));
			}
			res.add(new Pair<Vector<Pair<svm_node[], T>>, G>(ftrVectors, gold));
		}
		return res;
		
	}
	public List<Pair<Vector<Pair<FeaturePack<T>, T>>, G>> getDataAndFeaturePacksAndGoldOnePerInstance(){
		
		List<Pair<Vector<Pair<FeaturePack<T>, T>>, G>> res = new Vector<>();
		for (int i=0; i<featureVectorsAndTargetGroups.size(); i++){
			List<Pair<FeaturePack<T>, Double>> ftrPackAndTargets = featureVectorsAndTargetGroups.get(i);
			G gold = groupGolds.get(i);
			List<T> data = groupDatas.get(i);
			Vector<Pair<FeaturePack<T>, T>> ftrVectors = new Vector<Pair<FeaturePack<T>, T>>();
			for (int j=0; j<ftrPackAndTargets.size(); j++){
				Pair<FeaturePack<T>, Double> ftrPackAndTarget = ftrPackAndTargets.get(j);
				ftrVectors.add(new Pair<FeaturePack<T>, T>(
						ftrPackAndTarget.first,
						data.get(j)
						));
			}
			res.add(new Pair<Vector<Pair<FeaturePack<T>, T>>, G>(ftrVectors, gold));
		}
		return res;
		
	}

	private svm_problem createProblem(Vector<Double> targets,
			Vector<svm_node[]> ftrVectors) {
		svm_problem problem = new svm_problem();
		problem.l = targets.size();
		problem.x = new svm_node[problem.l][];
		for (int i = 0; i < problem.l; i++)
			problem.x[i] = ftrVectors.elementAt(i);
		problem.y = new double[problem.l];
		for (int i = 0; i < problem.l; i++)
			problem.y[i] = targets.elementAt(i);
		return problem;
	}

	/**
	 * Dump the examples to a file.
	 * 
	 * @param filename
	 *            where to write the dump.
	 * @throws IOException
	 *             in case of error while writing the file.
	 */
	public void dumpExamplesLibSvm(String filename, FeatureNormalizer fn) throws IOException {
		dumpExamplesLibSvm(filename, fn, null);
	}

	public void dumpExamplesLibSvm(String filename, FeatureNormalizer fn, int[] selectedFeatures) throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(filename, false));

		for (int i = 0; i < featureVectorsAndTargetGroups.size(); i++)
			for (Pair<FeaturePack<T>, Double> vectAndGold : featureVectorsAndTargetGroups.get(i))
				writeLineLibSvm(fn.ftrToNormalizedFtrArray(vectAndGold.first), wr, vectAndGold.second, i, selectedFeatures);
		wr.close();
	}

	private void writeLineLibSvm(double[] ftrVect, BufferedWriter wr, double gold, int id, int[] selectedFeatures)
			throws IOException {
		String line = String.format("%.5f ", gold);
		for (int ftr = 0; ftr < ftrVect.length; ftr++)
			if (selectedFeatures == null || ArrayUtils.contains(selectedFeatures, ftr + 1))
				line += String.format("%d:%.9f ", ftr + 1, ftrVect[ftr]);
		line += " #id=" + id;
		wr.write(line + "\n");
	}

	private void writeLineRankLib(double[] ftrVect, BufferedWriter wr,
			int rank, int groupid) throws IOException {
		String line = String.format("%d qid:%d ", rank, groupid);
		for (int ftr = 0; ftr < ftrVect.length; ftr++)
			line += String.format("%d:%.9f ", ftr + 1, ftrVect[ftr]);
		wr.write(line + "\n");
	}

	public void dumpExamplesRankLib(String filename, FeatureNormalizer fn)
			throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(filename, false));

		for (int groupId = 0; groupId < featureVectorsAndTargetGroups.size(); groupId++) {
			List<Pair<FeaturePack<T>, Double>> featureVectorsAndGolds = new Vector<>(
					featureVectorsAndTargetGroups.get(groupId));
			Collections.sort(featureVectorsAndGolds,
					new SmaphUtils.ComparePairsBySecondElement());
			Collections.reverse(featureVectorsAndGolds);
			int rank = 0;
			double lastVal = Double.NaN;
			for (Pair<FeaturePack<T>, Double> pair : featureVectorsAndGolds) {
				if (pair.second != lastVal) {
					lastVal = pair.second;
					rank++;
				}
				writeLineRankLib(fn.ftrToNormalizedFtrArray(pair.first), wr,
						rank, groupId);
			}

		}
		wr.close();
	}
}
