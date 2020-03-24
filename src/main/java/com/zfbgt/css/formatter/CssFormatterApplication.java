package com.zfbgt.css.formatter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @Description TODO 启动类
 * @author LaoQin
 * @date 2020/03/20
 */
@Slf4j
@SpringBootApplication
public class CssFormatterApplication {
	//将启动参数存起来
	public static String path;

	public static void main(String[] args) {
		log.info("项目启动...");
		//判断是否传入文件路径
		if(args.length==0){
			log.info("找不到文件...");
			log.info("终止程序！");
			//退出程序
			System.exit(-1);
		}
		log.info("文件路径为："+args[0]);
		path = args[0];
		SpringApplication.run(CssFormatterApplication.class, args);
	}

	@PostConstruct
	public void formatFile() throws IOException {
		log.info("开始处理文件...");
		log.info("获得文件路径为"+path);
		File file = new File(path);
		Document document = Jsoup.parse(file, "UTF-8");
		Element template = document.getElementsByTag("template").get(0);
		Element style = document.getElementsByTag("style").get(0);
		//判断style有没有设置lang属性
		String lang = style.attr("lang");
		if(StringUtils.isEmpty(lang)){
			log.info("用户没有设置scss属性");
			style.attr("lang","scss");
		}
		if(!StringUtils.isEmpty(style.text())){
			log.info("style中已有内容，终止程序！");
			//退出程序
			System.exit(-1);
		}
		String result = JSON.toJSONString(generateScss(new JSONObject(),template));
		result  = "page" + resultFilter(result);
		style.text(result);
		//过滤多余标签
		String tempStr = tagFilter(document.html(), "html","head","body");
		//转码
		String encodedStr = new String(tempStr.getBytes(), "UTF-8");
		FileWriter writer = new FileWriter(file);
		writer.write(encodedStr);
		writer.flush();
		writer.close();
	}

	/**
	 * @Description TODO 过滤最后json结果
	 * @author LaoQin
	 * @date 2020/03/20
	 * @param result 返回的结果
	 * @return java.lang.String
	 */
	private String resultFilter(String result) {
		return result.replace("\"","").replace(":","").replace(",","");
	}

	/**
	 * @Description TODO 过滤某特定标签
	 * @author LaoQin
	 * @date 2020/03/20
	 * @param str 待过滤标签
	 * @param tagNames 过滤标签集合
	 * @return java.lang.String 过滤后的标签
	 */
	private String tagFilter(String str,String ... tagNames){
		for(String tagName: tagNames){
			str = str.replace("<" + tagName + ">", "")
					.replace("</" + tagName + ">", "");
		}
		return str;
	}
	/**
	 * @Description TODO 生成Scss
	 * @author LaoQin
	 * @date 2020/03/20
	 * @param result 生成结果
	 * @param element 待生成元素
	 * @return void
	 */
	private JSONObject generateScss(JSONObject result, Element element) {
		Elements children = element.children();
		for(Element child : children){
			JSONObject childObj = generateScss(new JSONObject(), child);
			String classStr = child.attr("class");
			if(StringUtils.isEmpty(classStr)){
				//没有class默认用标签名
				result.put(child.tagName(),childObj);
				continue;
			}
			String[] splitedClassStr = classStr.split(" ");
			result.put("."+splitedClassStr[0],childObj);
		}
		return result;
	}
}
