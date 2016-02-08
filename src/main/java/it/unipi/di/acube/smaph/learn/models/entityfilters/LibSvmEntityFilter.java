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

package it.unipi.di.acube.smaph.learn.models.entityfilters;

import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.smaph.learn.featurePacks.FeaturePack;
import it.unipi.di.acube.smaph.learn.models.LibSvmModel;
import it.unipi.di.acube.smaph.learn.normalizer.FeatureNormalizer;

import java.io.IOException;

import libsvm.svm_model;

/**
 * An SVM-based entity filter.
 */
public class LibSvmEntityFilter extends LibSvmModel<Tag> implements EntityFilter {

	
	public LibSvmEntityFilter(String modelFile) throws IOException {
		super(modelFile);
	}

	public LibSvmEntityFilter(svm_model model) {
		super(model);
	}

	@Override
	public boolean filterEntity(FeaturePack<Tag> fp, FeatureNormalizer fn) {
		boolean result = predict(fp, fn);
		/*String ftrDesc = "";
		for (String key : fp.getFeatureNames())
			if (fp.featureIsSet(key))
				ftrDesc += String.format("%s:%.3f ", key, fp.getFeature(key));
		SmaphAnnotatorDebugger.out.printf("EF: %s has been %s.%n", ftrDesc,
				result ? "accepted" : "discarded");*/
		return result;
	}

}
