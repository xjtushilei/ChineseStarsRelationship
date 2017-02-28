package com.xjtushilei.main;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Created by shilei on 2017/2/27.
 */
public class Spider {

	private static final Logger logger = Logger.getLogger(Spider.class);
	private static BigInteger MaxNumber = new BigInteger("9999999999999999999999999");; // 最多爬取多少条关系停止
	private static String rootPath = "D://互动百科/";

	private static Set<String> nameSet = new HashSet<String>();
	private static Queue<String> nameQueue = new LinkedList<String>();
	private static BigInteger count = new BigInteger("0");

	/**
	 * 搜索函数，主要处理路径
	 */
	public static void strat() {
		
		// 删除存在的文件，重新开始
		try {
			FileUtils.deleteDirectory(new File(rootPath));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			// 将自己想要开始的明星加入到队列中
			List<String> namelist = FileUtils
					.readLines(new File(Spider.class.getClassLoader().getResource("start_list").getFile()), "utf-8");
			for (String string : namelist) {
				nameQueue.offer(string);
			}
			logger.info("一共 【" + namelist.size() + "】 加入开始队列！");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 开始bfs爬虫
		while (!nameQueue.isEmpty() && count.compareTo(MaxNumber) == -1) {
			String name=nameQueue.poll();
			if (name.equals("")) {
				continue;
			}
			go(name);
		}
	}

	private static void go(String name) {
		logger.info(name + "：start nameSet:" + nameSet.size() + "	nameQueue:" + nameQueue.size());
		Document doc = null;
		try {
			doc = Jsoup.connect("http://www.baike.com/wiki/" + name)
					.userAgent("Mozilla/5.0 (Windows NT 6.1; rv:22.0) Gecko/20100101 Firefox/22.0")
					.ignoreContentType(true).timeout(30000).get();

			// 选择器 选到制定的位置
			Elements relationships = doc.select("#figurerelation li");
			for (Element li : relationships) {
				// 获取相关的信息
				Element other = li.select("a").first();
				String otherName = other.text();
				String relationShip = li.ownText();
				if (relationShip.equals("")) {
					relationShip="朋友";
				}
				if (nameSet.add(otherName)) {
					nameQueue.add(otherName);
					logger.debug("队列增加：" + otherName);
					// 写文件
					try {
						FileUtils.write(new File(rootPath + "Relationship.data"),
								name + "\t" + relationShip + "\t" + otherName + "\n", "utf-8", true);
						count = count.add(new BigInteger("1"));
						logger.info("当前成功数目：【" + count.toString() + "】");
					} catch (IOException e) {
						logger.error("写入：" + name + "-" + relationShip + "-" + otherName + "\t\t失败！");
					}
				}

			}
		} catch (IOException e) {
			logger.error("" + name + "\t失败 ！  重新加入队列！");
			nameQueue.add(name);// 超时之后，爬取失败，所以又一次加入了队列中
			return;
		}
		// 下载这个人summary信息
		downloadSummary(name, doc);
		// 下载这个人的图片信息
		downloadPicture(name, doc);
	}

	private static void downloadSummary(String name, Document doc) {
		
		// 写文件
		try {
			String summary = doc.select(".summary p").first().text();
			logger.debug(summary);
			FileUtils.write(new File(rootPath + "Summary.data"), name + "\n" + summary + "\n", "utf-8", true);
		} catch (Exception e) {
			logger.error("个人描述信息 写入：【" + name + "】\t失败！");
			try {
				FileUtils.write(new File(rootPath + "ErrorSummary.data"), name + "\t" + e.toString() + "\n", "utf-8", true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private static void downloadPicture(String name, Document doc) {
		String picUrl = null;
		try {
			picUrl = doc.select(".doc-img a img").first().attr("src");
			FileUtils.copyURLToFile(new URL(picUrl), new File(rootPath + "img/" + name + ".jpg"));
			logger.info(name + " 【图片】 下载成功");
		} catch (Exception e) {
			logger.error(name + " 【图片】 下载失败！ ");
			try {
				FileUtils.write(new File(rootPath + "ErrorPicture.data"), name + "\t" + picUrl +"\t"+e.toString()+  "\n", "utf-8", true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
