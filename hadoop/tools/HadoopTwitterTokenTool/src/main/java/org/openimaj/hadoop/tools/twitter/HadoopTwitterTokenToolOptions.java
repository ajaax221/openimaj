/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.hadoop.tools.twitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.hadoop.tools.HadoopToolsUtil;
import org.openimaj.hadoop.tools.twitter.token.mode.TwitterTokenModeOption;
import org.openimaj.tools.InOutToolOptions;

/**
 * Hadoop specific options for twitter preprocessing
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>, Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class HadoopTwitterTokenToolOptions extends InOutToolOptions{
	@Option(name="--mode", aliases="-m", required=true, usage="How should the tweet tokens should be counted and processed.", handler=ProxyOptionHandler.class, multiValued=true)
	List<TwitterTokenModeOption> modeOptions = new ArrayList<TwitterTokenModeOption>();
	
	@Option(name="--json-path", aliases="-j", required=true, usage="A JSONPath query defining the field to find tokens to count", metaVar="STRING")
	String tokensJSONPath;
	
	@Option(name="--time-delta", aliases="-t", required=false, usage="The length of a time window in minutes (defaults to 1 hour (60))", metaVar="STRING")
	private long timeDelta = 60;

	private String[] args;
	
	private boolean  beforeMaps;

	
	
	public HadoopTwitterTokenToolOptions(String[] args,boolean beforeMaps) throws CmdLineException {
		this.args = args;
		this.beforeMaps = beforeMaps;
		if(this.beforeMaps)
			this.prepareCL();
		else
			this.prepare();
	}
	
	public HadoopTwitterTokenToolOptions(String[] args) throws CmdLineException {
		this(args,false);
	}
	
	/**
	 * prepare the tool for running (command line version)
	 */
	public void prepareCL(){
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			prepareMultivaluedArgument(modeOptions);
			this.validate();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar JClusterQuantiser.jar [options...] [files...]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	/**
	 * @throws CmdLineException
	 */
	public void prepare() throws CmdLineException{
		CmdLineParser parser = new CmdLineParser(this);
		parser.parseArgument(args);
		prepareMultivaluedArgument(modeOptions);
//			System.out.println(Arrays.toString(args));
		System.out.println("Number of mode options: " + modeOptions.size());
		this.validate();
	}

	private void validate() throws CmdLineException {
		if(this.beforeMaps)
		{
			HadoopToolsUtil.validateInput(this);
			HadoopToolsUtil.validateOutput(this);
		}
	}

	/**
	 * @return the delta between time windows in minutes
	 */
	public long getTimeDelta() {
		return this.timeDelta;
	}

	public String getJsonPath() {
		return this.tokensJSONPath;
	}

	public String[] getArgs() {
		return args;
	}
}