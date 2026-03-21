package cn.wubo.spring.ai.mcp.cn.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherService {

    private static final String BASE_URL = "http://t.weather.itboy.net/api/weather/city/";

    private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final Map<String, CityCodeInfo> cityCodeMap;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
		this.objectMapper = new ObjectMapper();
		this.cityCodeMap = loadCityCodes();
    }

	private static Map<String, CityCodeInfo> loadCityCodes() {
		List<CityCodeInfo> cityCodes = Arrays.asList(
			new CityCodeInfo(1, "北京", "北京", "101010100"),
			new CityCodeInfo(2, "北京", "朝阳", "101010300"),
			new CityCodeInfo(3, "北京", "顺义", "101010400"),
			new CityCodeInfo(4, "北京", "怀柔", "101010500"),
			new CityCodeInfo(5, "北京", "通州", "101010600"),
			new CityCodeInfo(6, "北京", "昌平", "101010700"),
			new CityCodeInfo(7, "北京", "延庆", "101010800"),
			new CityCodeInfo(8, "北京", "丰台", "101010900"),
			new CityCodeInfo(9, "北京", "石景山", "101011000"),
			new CityCodeInfo(10, "北京", "大兴", "101011100"),
			new CityCodeInfo(11, "北京", "房山", "101011200"),
			new CityCodeInfo(12, "北京", "密云", "101011300"),
			new CityCodeInfo(13, "北京", "门头沟", "101011400"),
			new CityCodeInfo(14, "北京", "平谷", "101011500"),
			new CityCodeInfo(15, "北京", "八达岭", "101011600"),
			new CityCodeInfo(16, "北京", "佛爷顶", "101011700"),
			new CityCodeInfo(17, "北京", "汤河口", "101011800"),
			new CityCodeInfo(18, "北京", "密云上甸子", "101011900"),
			new CityCodeInfo(19, "北京", "斋堂", "101012000"),
			new CityCodeInfo(20, "北京", "霞云岭", "101012100"),
			new CityCodeInfo(21, "北京", "北京城区", "101012200"),
			new CityCodeInfo(22, "北京", "海淀", "101010200"),
			new CityCodeInfo(23, "天津市", "天津", "101030100"),
			new CityCodeInfo(24, "天津市", "宝坻", "101030300"),
			new CityCodeInfo(25, "天津市", "东丽", "101030400"),
			new CityCodeInfo(26, "天津市", "西青", "101030500"),
			new CityCodeInfo(27, "天津市", "北辰", "101030600"),
			new CityCodeInfo(28, "天津市", "蓟县", "101031400"),
			new CityCodeInfo(29, "天津市", "汉沽", "101030800"),
			new CityCodeInfo(30, "天津市", "静海", "101030900"),
			new CityCodeInfo(31, "天津市", "津南", "101031000"),
			new CityCodeInfo(32, "天津市", "塘沽", "101031100"),
			new CityCodeInfo(33, "天津市", "大港", "101031200"),
			new CityCodeInfo(34, "天津市", "武清", "101030200"),
			new CityCodeInfo(35, "天津市", "宁河", "101030700"),
			new CityCodeInfo(36, "上海", "上海", "101020100"),
			new CityCodeInfo(37, "上海", "宝山", "101020300"),
			new CityCodeInfo(38, "上海", "嘉定", "101020500"),
			new CityCodeInfo(39, "上海", "南汇", "101020600"),
			new CityCodeInfo(40, "上海", "浦东", "101021300"),
			new CityCodeInfo(41, "上海", "青浦", "101020800"),
			new CityCodeInfo(42, "上海", "松江", "101020900"),
			new CityCodeInfo(43, "上海", "奉贤", "101021000"),
			new CityCodeInfo(44, "上海", "崇明", "101021100"),
			new CityCodeInfo(45, "上海", "徐家汇", "101021200"),
			new CityCodeInfo(46, "上海", "闵行", "101020200"),
			new CityCodeInfo(47, "上海", "金山", "101020700"),
			new CityCodeInfo(48, "河北", "石家庄", "101090101"),
			new CityCodeInfo(49, "河北", "张家口", "101090301"),
			new CityCodeInfo(50, "河北", "承德", "101090402"),
			new CityCodeInfo(51, "河北", "唐山", "101090501"),
			new CityCodeInfo(52, "河北", "秦皇岛", "101091101"),
			new CityCodeInfo(53, "河北", "沧州", "101090701"),
			new CityCodeInfo(54, "河北", "衡水", "101090801"),
			new CityCodeInfo(55, "河北", "邢台", "101090901"),
			new CityCodeInfo(56, "河北", "邯郸", "101091001"),
			new CityCodeInfo(57, "河北", "保定", "101090201"),
			new CityCodeInfo(58, "河北", "廊坊", "101090601"),
			new CityCodeInfo(59, "河南", "郑州", "101180101"),
			new CityCodeInfo(60, "河南", "新乡", "101180301"),
			new CityCodeInfo(61, "河南", "许昌", "101180401"),
			new CityCodeInfo(62, "河南", "平顶山", "101180501"),
			new CityCodeInfo(63, "河南", "信阳", "101180601"),
			new CityCodeInfo(64, "河南", "南阳", "101180701"),
			new CityCodeInfo(65, "河南", "开封", "101180801"),
			new CityCodeInfo(66, "河南", "洛阳", "101180901"),
			new CityCodeInfo(67, "河南", "商丘", "101181001"),
			new CityCodeInfo(68, "河南", "焦作", "101181101"),
			new CityCodeInfo(69, "河南", "鹤壁", "101181201"),
			new CityCodeInfo(70, "河南", "濮阳", "101181301"),
			new CityCodeInfo(71, "河南", "周口", "101181401"),
			new CityCodeInfo(72, "河南", "漯河", "101181501"),
			new CityCodeInfo(73, "河南", "驻马店", "101181601"),
			new CityCodeInfo(74, "河南", "三门峡", "101181701"),
			new CityCodeInfo(75, "河南", "济源", "101181801"),
			new CityCodeInfo(76, "河南", "安阳", "101180201"),
			new CityCodeInfo(77, "安徽", "合肥", "101220101"),
			new CityCodeInfo(78, "安徽", "芜湖", "101220301"),
			new CityCodeInfo(79, "安徽", "淮南", "101220401"),
			new CityCodeInfo(80, "安徽", "马鞍山", "101220501"),
			new CityCodeInfo(81, "安徽", "安庆", "101220601"),
			new CityCodeInfo(82, "安徽", "宿州", "101220701"),
			new CityCodeInfo(83, "安徽", "阜阳", "101220801"),
			new CityCodeInfo(84, "安徽", "亳州", "101220901"),
			new CityCodeInfo(85, "安徽", "黄山", "101221001"),
			new CityCodeInfo(86, "安徽", "滁州", "101221101"),
			new CityCodeInfo(87, "安徽", "淮北", "101221201"),
			new CityCodeInfo(88, "安徽", "铜陵", "101221301"),
			new CityCodeInfo(89, "安徽", "宣城", "101221401"),
			new CityCodeInfo(90, "安徽", "六安", "101221501"),
			new CityCodeInfo(91, "安徽", "巢湖", "101221601"),
			new CityCodeInfo(92, "安徽", "池州", "101221701"),
			new CityCodeInfo(93, "安徽", "蚌埠", "101220201"),
			new CityCodeInfo(94, "浙江", "杭州", "101210101"),
			new CityCodeInfo(95, "浙江", "舟山", "101211101"),
			new CityCodeInfo(96, "浙江", "湖州", "101210201"),
			new CityCodeInfo(97, "浙江", "嘉兴", "101210301"),
			new CityCodeInfo(98, "浙江", "金华", "101210901"),
			new CityCodeInfo(99, "浙江", "绍兴", "101210501"),
			new CityCodeInfo(100, "浙江", "台州", "101210601"),
			new CityCodeInfo(101, "浙江", "温州", "101210701"),
			new CityCodeInfo(102, "浙江", "丽水", "101210801"),
			new CityCodeInfo(103, "浙江", "衢州", "101211001"),
			new CityCodeInfo(104, "浙江", "宁波", "101210401"),
			new CityCodeInfo(105, "重庆", "重庆", "101040100"),
			new CityCodeInfo(106, "重庆", "合川", "101040300"),
			new CityCodeInfo(107, "重庆", "南川", "101040400"),
			new CityCodeInfo(108, "重庆", "江津", "101040500"),
			new CityCodeInfo(109, "重庆", "万盛", "101040600"),
			new CityCodeInfo(110, "重庆", "渝北", "101040700"),
			new CityCodeInfo(111, "重庆", "北碚", "101040800"),
			new CityCodeInfo(112, "重庆", "巴南", "101040900"),
			new CityCodeInfo(113, "重庆", "长寿", "101041000"),
			new CityCodeInfo(114, "重庆", "黔江", "101041100"),
			new CityCodeInfo(115, "重庆", "万州天城", "101041200"),
			new CityCodeInfo(116, "重庆", "万州龙宝", "101041300"),
			new CityCodeInfo(117, "重庆", "涪陵", "101041400"),
			new CityCodeInfo(118, "重庆", "开县", "101041500"),
			new CityCodeInfo(119, "重庆", "城口", "101041600"),
			new CityCodeInfo(120, "重庆", "云阳", "101041700"),
			new CityCodeInfo(121, "重庆", "巫溪", "101041800"),
			new CityCodeInfo(122, "重庆", "奉节", "101041900"),
			new CityCodeInfo(123, "重庆", "巫山", "101042000"),
			new CityCodeInfo(124, "重庆", "潼南", "101042100"),
			new CityCodeInfo(125, "重庆", "垫江", "101042200"),
			new CityCodeInfo(126, "重庆", "梁平", "101042300"),
			new CityCodeInfo(127, "重庆", "忠县", "101042400"),
			new CityCodeInfo(128, "重庆", "石柱", "101042500"),
			new CityCodeInfo(129, "重庆", "大足", "101042600"),
			new CityCodeInfo(130, "重庆", "荣昌", "101042700"),
			new CityCodeInfo(131, "重庆", "铜梁", "101042800"),
			new CityCodeInfo(132, "重庆", "璧山", "101042900"),
			new CityCodeInfo(133, "重庆", "丰都", "101043000"),
			new CityCodeInfo(134, "重庆", "武隆", "101043100"),
			new CityCodeInfo(135, "重庆", "彭水", "101043200"),
			new CityCodeInfo(136, "重庆", "綦江", "101043300"),
			new CityCodeInfo(137, "重庆", "酉阳", "101043400"),
			new CityCodeInfo(138, "重庆", "秀山", "101043600"),
			new CityCodeInfo(139, "重庆", "沙坪坝", "101043700"),
			new CityCodeInfo(140, "重庆", "永川", "101040200"),
			new CityCodeInfo(141, "福建", "福州", "101230101"),
			new CityCodeInfo(142, "福建", "泉州", "101230501"),
			new CityCodeInfo(143, "福建", "漳州", "101230601"),
			new CityCodeInfo(144, "福建", "龙岩", "101230701"),
			new CityCodeInfo(145, "福建", "晋江", "101230509"),
			new CityCodeInfo(146, "福建", "南平", "101230901"),
			new CityCodeInfo(147, "福建", "厦门", "101230201"),
			new CityCodeInfo(148, "福建", "宁德", "101230301"),
			new CityCodeInfo(149, "福建", "莆田", "101230401"),
			new CityCodeInfo(150, "福建", "三明", "101230801"),
			new CityCodeInfo(151, "甘肃", "兰州", "101160101"),
			new CityCodeInfo(152, "甘肃", "平凉", "101160301"),
			new CityCodeInfo(153, "甘肃", "庆阳", "101160401"),
			new CityCodeInfo(154, "甘肃", "武威", "101160501"),
			new CityCodeInfo(155, "甘肃", "金昌", "101160601"),
			new CityCodeInfo(156, "甘肃", "嘉峪关", "101161401"),
			new CityCodeInfo(157, "甘肃", "酒泉", "101160801"),
			new CityCodeInfo(158, "甘肃", "天水", "101160901"),
			new CityCodeInfo(159, "甘肃", "武都", "101161001"),
			new CityCodeInfo(160, "甘肃", "临夏", "101161101"),
			new CityCodeInfo(161, "甘肃", "合作", "101161201"),
			new CityCodeInfo(162, "甘肃", "白银", "101161301"),
			new CityCodeInfo(163, "甘肃", "定西", "101160201"),
			new CityCodeInfo(164, "甘肃", "张掖", "101160701"),
			new CityCodeInfo(165, "广东", "广州", "101280101"),
			new CityCodeInfo(166, "广东", "惠州", "101280301"),
			new CityCodeInfo(167, "广东", "梅州", "101280401"),
			new CityCodeInfo(168, "广东", "汕头", "101280501"),
			new CityCodeInfo(169, "广东", "深圳", "101280601"),
			new CityCodeInfo(170, "广东", "珠海", "101280701"),
			new CityCodeInfo(171, "广东", "佛山", "101280800"),
			new CityCodeInfo(172, "广东", "肇庆", "101280901"),
			new CityCodeInfo(173, "广东", "湛江", "101281001"),
			new CityCodeInfo(174, "广东", "江门", "101281101"),
			new CityCodeInfo(175, "广东", "河源", "101281201"),
			new CityCodeInfo(176, "广东", "清远", "101281301"),
			new CityCodeInfo(177, "广东", "云浮", "101281401"),
			new CityCodeInfo(178, "广东", "潮州", "101281501"),
			new CityCodeInfo(179, "广东", "东莞", "101281601"),
			new CityCodeInfo(180, "广东", "中山", "101281701"),
			new CityCodeInfo(181, "广东", "阳江", "101281801"),
			new CityCodeInfo(182, "广东", "揭阳", "101281901"),
			new CityCodeInfo(183, "广东", "茂名", "101282001"),
			new CityCodeInfo(184, "广东", "汕尾", "101282101"),
			new CityCodeInfo(185, "广东", "韶关", "101280201"),
			new CityCodeInfo(186, "广西", "南宁", "101300101"),
			new CityCodeInfo(187, "广西", "柳州", "101300301"),
			new CityCodeInfo(188, "广西", "来宾", "101300401"),
			new CityCodeInfo(189, "广西", "桂林", "101300501"),
			new CityCodeInfo(190, "广西", "梧州", "101300601"),
			new CityCodeInfo(191, "广西", "防城港", "101301401"),
			new CityCodeInfo(192, "广西", "贵港", "101300801"),
			new CityCodeInfo(193, "广西", "玉林", "101300901"),
			new CityCodeInfo(194, "广西", "百色", "101301001"),
			new CityCodeInfo(195, "广西", "钦州", "101301101"),
			new CityCodeInfo(196, "广西", "河池", "101301201"),
			new CityCodeInfo(197, "广西", "北海", "101301301"),
			new CityCodeInfo(198, "广西", "崇左", "101300201"),
			new CityCodeInfo(199, "广西", "贺州", "101300701"),
			new CityCodeInfo(200, "贵州", "贵阳", "101260101"),
			new CityCodeInfo(201, "贵州", "安顺", "101260301"),
			new CityCodeInfo(202, "贵州", "都匀", "101260401"),
			new CityCodeInfo(203, "贵州", "兴义", "101260906"),
			new CityCodeInfo(204, "贵州", "铜仁", "101260601"),
			new CityCodeInfo(205, "贵州", "毕节", "101260701"),
			new CityCodeInfo(206, "贵州", "六盘水", "101260801"),
			new CityCodeInfo(207, "贵州", "遵义", "101260201"),
			new CityCodeInfo(208, "贵州", "凯里", "101260501"),
			new CityCodeInfo(209, "云南", "昆明", "101290101"),
			new CityCodeInfo(210, "云南", "红河", "101290301"),
			new CityCodeInfo(211, "云南", "文山", "101290601"),
			new CityCodeInfo(212, "云南", "玉溪", "101290701"),
			new CityCodeInfo(213, "云南", "楚雄", "101290801"),
			new CityCodeInfo(214, "云南", "普洱", "101290901"),
			new CityCodeInfo(215, "云南", "昭通", "101291001"),
			new CityCodeInfo(216, "云南", "临沧", "101291101"),
			new CityCodeInfo(217, "云南", "怒江", "101291201"),
			new CityCodeInfo(218, "云南", "香格里拉", "101291301"),
			new CityCodeInfo(219, "云南", "丽江", "101291401"),
			new CityCodeInfo(220, "云南", "德宏", "101291501"),
			new CityCodeInfo(221, "云南", "景洪", "101291601"),
			new CityCodeInfo(222, "云南", "大理", "101290201"),
			new CityCodeInfo(223, "云南", "曲靖", "101290401"),
			new CityCodeInfo(224, "云南", "保山", "101290501"),
			new CityCodeInfo(225, "内蒙古", "呼和浩特", "101080101"),
			new CityCodeInfo(226, "内蒙古", "乌海", "101080301"),
			new CityCodeInfo(227, "内蒙古", "集宁", "101080401"),
			new CityCodeInfo(228, "内蒙古", "通辽", "101080501"),
			new CityCodeInfo(229, "内蒙古", "阿拉善左旗", "101081201"),
			new CityCodeInfo(230, "内蒙古", "鄂尔多斯", "101080701"),
			new CityCodeInfo(231, "内蒙古", "临河", "101080801"),
			new CityCodeInfo(232, "内蒙古", "锡林浩特", "101080901"),
			new CityCodeInfo(233, "内蒙古", "呼伦贝尔", "101081000"),
			new CityCodeInfo(234, "内蒙古", "乌兰浩特", "101081101"),
			new CityCodeInfo(235, "内蒙古", "包头", "101080201"),
			new CityCodeInfo(236, "内蒙古", "赤峰", "101080601"),
			new CityCodeInfo(237, "江西", "南昌", "101240101"),
			new CityCodeInfo(238, "江西", "上饶", "101240301"),
			new CityCodeInfo(239, "江西", "抚州", "101240401"),
			new CityCodeInfo(240, "江西", "宜春", "101240501"),
			new CityCodeInfo(241, "江西", "鹰潭", "101241101"),
			new CityCodeInfo(242, "江西", "赣州", "101240701"),
			new CityCodeInfo(243, "江西", "景德镇", "101240801"),
			new CityCodeInfo(244, "江西", "萍乡", "101240901"),
			new CityCodeInfo(245, "江西", "新余", "101241001"),
			new CityCodeInfo(246, "江西", "九江", "101240201"),
			new CityCodeInfo(247, "江西", "吉安", "101240601"),
			new CityCodeInfo(248, "湖北", "武汉", "101200101"),
			new CityCodeInfo(249, "湖北", "黄冈", "101200501"),
			new CityCodeInfo(250, "湖北", "荆州", "101200801"),
			new CityCodeInfo(251, "湖北", "宜昌", "101200901"),
			new CityCodeInfo(252, "湖北", "恩施", "101201001"),
			new CityCodeInfo(253, "湖北", "十堰", "101201101"),
			new CityCodeInfo(254, "湖北", "神农架", "101201201"),
			new CityCodeInfo(255, "湖北", "随州", "101201301"),
			new CityCodeInfo(256, "湖北", "荆门", "101201401"),
			new CityCodeInfo(257, "湖北", "天门", "101201501"),
			new CityCodeInfo(258, "湖北", "仙桃", "101201601"),
			new CityCodeInfo(259, "湖北", "潜江", "101201701"),
			new CityCodeInfo(260, "湖北", "襄樊", "101200201"),
			new CityCodeInfo(261, "湖北", "鄂州", "101200301"),
			new CityCodeInfo(262, "湖北", "孝感", "101200401"),
			new CityCodeInfo(263, "湖北", "黄石", "101200601"),
			new CityCodeInfo(264, "湖北", "咸宁", "101200701"),
			new CityCodeInfo(265, "四川", "成都", "101270101"),
			new CityCodeInfo(266, "四川", "自贡", "101270301"),
			new CityCodeInfo(267, "四川", "绵阳", "101270401"),
			new CityCodeInfo(268, "四川", "南充", "101270501"),
			new CityCodeInfo(269, "四川", "达州", "101270601"),
			new CityCodeInfo(270, "四川", "遂宁", "101270701"),
			new CityCodeInfo(271, "四川", "广安", "101270801"),
			new CityCodeInfo(272, "四川", "巴中", "101270901"),
			new CityCodeInfo(273, "四川", "泸州", "101271001"),
			new CityCodeInfo(274, "四川", "宜宾", "101271101"),
			new CityCodeInfo(275, "四川", "内江", "101271201"),
			new CityCodeInfo(276, "四川", "资阳", "101271301"),
			new CityCodeInfo(277, "四川", "乐山", "101271401"),
			new CityCodeInfo(278, "四川", "眉山", "101271501"),
			new CityCodeInfo(279, "四川", "凉山", "101271601"),
			new CityCodeInfo(280, "四川", "雅安", "101271701"),
			new CityCodeInfo(281, "四川", "甘孜", "101271801"),
			new CityCodeInfo(282, "四川", "阿坝", "101271901"),
			new CityCodeInfo(283, "四川", "德阳", "101272001"),
			new CityCodeInfo(284, "四川", "广元", "101272101"),
			new CityCodeInfo(285, "四川", "攀枝花", "101270201"),
			new CityCodeInfo(286, "宁夏", "银川", "101170101"),
			new CityCodeInfo(287, "宁夏", "中卫", "101170501"),
			new CityCodeInfo(288, "宁夏", "固原", "101170401"),
			new CityCodeInfo(289, "宁夏", "石嘴山", "101170201"),
			new CityCodeInfo(290, "宁夏", "吴忠", "101170301"),
			new CityCodeInfo(291, "青海省", "西宁", "101150101"),
			new CityCodeInfo(292, "青海省", "黄南", "101150301"),
			new CityCodeInfo(293, "青海省", "海北", "101150801"),
			new CityCodeInfo(294, "青海省", "果洛", "101150501"),
			new CityCodeInfo(295, "青海省", "玉树", "101150601"),
			new CityCodeInfo(296, "青海省", "海西", "101150701"),
			new CityCodeInfo(297, "青海省", "海东", "101150201"),
			new CityCodeInfo(298, "青海省", "海南", "101150401"),
			new CityCodeInfo(299, "山东", "济南", "101120101"),
			new CityCodeInfo(300, "山东", "潍坊", "101120601"),
			new CityCodeInfo(301, "山东", "临沂", "101120901"),
			new CityCodeInfo(302, "山东", "菏泽", "101121001"),
			new CityCodeInfo(303, "山东", "滨州", "101121101"),
			new CityCodeInfo(304, "山东", "东营", "101121201"),
			new CityCodeInfo(305, "山东", "威海", "101121301"),
			new CityCodeInfo(306, "山东", "枣庄", "101121401"),
			new CityCodeInfo(307, "山东", "日照", "101121501"),
			new CityCodeInfo(308, "山东", "莱芜", "101121601"),
			new CityCodeInfo(309, "山东", "聊城", "101121701"),
			new CityCodeInfo(310, "山东", "青岛", "101120201"),
			new CityCodeInfo(311, "山东", "淄博", "101120301"),
			new CityCodeInfo(312, "山东", "德州", "101120401"),
			new CityCodeInfo(313, "山东", "烟台", "101120501"),
			new CityCodeInfo(314, "山东", "济宁", "101120701"),
			new CityCodeInfo(315, "山东", "泰安", "101120801"),
			new CityCodeInfo(316, "陕西省", "西安", "101110101"),
			new CityCodeInfo(317, "陕西省", "延安", "101110300"),
			new CityCodeInfo(318, "陕西省", "榆林", "101110401"),
			new CityCodeInfo(319, "陕西省", "铜川", "101111001"),
			new CityCodeInfo(320, "陕西省", "商洛", "101110601"),
			new CityCodeInfo(321, "陕西省", "安康", "101110701"),
			new CityCodeInfo(322, "陕西省", "汉中", "101110801"),
			new CityCodeInfo(323, "陕西省", "宝鸡", "101110901"),
			new CityCodeInfo(324, "陕西省", "咸阳", "101110200"),
			new CityCodeInfo(325, "陕西省", "渭南", "101110501"),
			new CityCodeInfo(326, "山西", "太原", "101100101"),
			new CityCodeInfo(327, "山西", "临汾", "101100701"),
			new CityCodeInfo(328, "山西", "运城", "101100801"),
			new CityCodeInfo(329, "山西", "朔州", "101100901"),
			new CityCodeInfo(330, "山西", "忻州", "101101001"),
			new CityCodeInfo(331, "山西", "长治", "101100501"),
			new CityCodeInfo(332, "山西", "大同", "101100201"),
			new CityCodeInfo(333, "山西", "阳泉", "101100301"),
			new CityCodeInfo(334, "山西", "晋中", "101100401"),
			new CityCodeInfo(335, "山西", "晋城", "101100601"),
			new CityCodeInfo(336, "山西", "吕梁", "101101100"),
			new CityCodeInfo(337, "新疆", "乌鲁木齐", "101130101"),
			new CityCodeInfo(338, "新疆", "石河子", "101130301"),
			new CityCodeInfo(339, "新疆", "昌吉", "101130401"),
			new CityCodeInfo(340, "新疆", "吐鲁番", "101130501"),
			new CityCodeInfo(341, "新疆", "库尔勒", "101130601"),
			new CityCodeInfo(342, "新疆", "阿拉尔", "101130701"),
			new CityCodeInfo(343, "新疆", "阿克苏", "101130801"),
			new CityCodeInfo(344, "新疆", "喀什", "101130901"),
			new CityCodeInfo(345, "新疆", "伊宁", "101131001"),
			new CityCodeInfo(346, "新疆", "塔城", "101131101"),
			new CityCodeInfo(347, "新疆", "哈密", "101131201"),
			new CityCodeInfo(348, "新疆", "和田", "101131301"),
			new CityCodeInfo(349, "新疆", "阿勒泰", "101131401"),
			new CityCodeInfo(350, "新疆", "阿图什", "101131501"),
			new CityCodeInfo(351, "新疆", "博乐", "101131601"),
			new CityCodeInfo(352, "新疆", "克拉玛依", "101130201"),
			new CityCodeInfo(353, "西藏", "拉萨", "101140101"),
			new CityCodeInfo(354, "西藏", "山南", "101140301"),
			new CityCodeInfo(355, "西藏", "阿里", "101140701"),
			new CityCodeInfo(356, "西藏", "昌都", "101140501"),
			new CityCodeInfo(357, "西藏", "那曲", "101140601"),
			new CityCodeInfo(358, "西藏", "日喀则", "101140201"),
			new CityCodeInfo(359, "西藏", "林芝", "101140401"),
			new CityCodeInfo(360, "台湾", "台北县", "101340101"),
			new CityCodeInfo(361, "台湾", "高雄", "101340201"),
			new CityCodeInfo(362, "台湾", "台中", "101340401"),
			new CityCodeInfo(363, "海南省", "海口", "101310101"),
			new CityCodeInfo(364, "海南省", "三亚", "101310201"),
			new CityCodeInfo(365, "海南省", "东方", "101310202"),
			new CityCodeInfo(366, "海南省", "临高", "101310203"),
			new CityCodeInfo(367, "海南省", "澄迈", "101310204"),
			new CityCodeInfo(368, "海南省", "儋州", "101310205"),
			new CityCodeInfo(369, "海南省", "昌江", "101310206"),
			new CityCodeInfo(370, "海南省", "白沙", "101310207"),
			new CityCodeInfo(371, "海南省", "琼中", "101310208"),
			new CityCodeInfo(372, "海南省", "定安", "101310209"),
			new CityCodeInfo(373, "海南省", "屯昌", "101310210"),
			new CityCodeInfo(374, "海南省", "琼海", "101310211"),
			new CityCodeInfo(375, "海南省", "文昌", "101310212"),
			new CityCodeInfo(376, "海南省", "保亭", "101310214"),
			new CityCodeInfo(377, "海南省", "万宁", "101310215"),
			new CityCodeInfo(378, "海南省", "陵水", "101310216"),
			new CityCodeInfo(379, "海南省", "西沙", "101310217"),
			new CityCodeInfo(380, "海南省", "南沙岛", "101310220"),
			new CityCodeInfo(381, "海南省", "乐东", "101310221"),
			new CityCodeInfo(382, "海南省", "五指山", "101310222"),
			new CityCodeInfo(383, "海南省", "琼山", "101310102"),
			new CityCodeInfo(384, "湖南", "长沙", "101250101"),
			new CityCodeInfo(385, "湖南", "株洲", "101250301"),
			new CityCodeInfo(386, "湖南", "衡阳", "101250401"),
			new CityCodeInfo(387, "湖南", "郴州", "101250501"),
			new CityCodeInfo(388, "湖南", "常德", "101250601"),
			new CityCodeInfo(389, "湖南", "益阳", "101250700"),
			new CityCodeInfo(390, "湖南", "娄底", "101250801"),
			new CityCodeInfo(391, "湖南", "邵阳", "101250901"),
			new CityCodeInfo(392, "湖南", "岳阳", "101251001"),
			new CityCodeInfo(393, "湖南", "张家界", "101251101"),
			new CityCodeInfo(394, "湖南", "怀化", "101251201"),
			new CityCodeInfo(395, "湖南", "黔阳", "101251301"),
			new CityCodeInfo(396, "湖南", "永州", "101251401"),
			new CityCodeInfo(397, "湖南", "吉首", "101251501"),
			new CityCodeInfo(398, "湖南", "湘潭", "101250201"),
			new CityCodeInfo(399, "江苏", "南京", "101190101"),
			new CityCodeInfo(400, "江苏", "镇江", "101190301"),
			new CityCodeInfo(401, "江苏", "苏州", "101190401"),
			new CityCodeInfo(402, "江苏", "南通", "101190501"),
			new CityCodeInfo(403, "江苏", "扬州", "101190601"),
			new CityCodeInfo(404, "江苏", "宿迁", "101191301"),
			new CityCodeInfo(405, "江苏", "徐州", "101190801"),
			new CityCodeInfo(406, "江苏", "淮安", "101190901"),
			new CityCodeInfo(407, "江苏", "连云港", "101191001"),
			new CityCodeInfo(408, "江苏", "常州", "101191101"),
			new CityCodeInfo(409, "江苏", "泰州", "101191201"),
			new CityCodeInfo(410, "江苏", "无锡", "101190201"),
			new CityCodeInfo(411, "江苏", "盐城", "101190701"),
			new CityCodeInfo(412, "黑龙江", "哈尔滨", "101050101"),
			new CityCodeInfo(413, "黑龙江", "牡丹江", "101050301"),
			new CityCodeInfo(414, "黑龙江", "佳木斯", "101050401"),
			new CityCodeInfo(415, "黑龙江", "绥化", "101050501"),
			new CityCodeInfo(416, "黑龙江", "黑河", "101050601"),
			new CityCodeInfo(417, "黑龙江", "双鸭山", "101051301"),
			new CityCodeInfo(418, "黑龙江", "伊春", "101050801"),
			new CityCodeInfo(419, "黑龙江", "大庆", "101050901"),
			new CityCodeInfo(420, "黑龙江", "七台河", "101051002"),
			new CityCodeInfo(421, "黑龙江", "鸡西", "101051101"),
			new CityCodeInfo(422, "黑龙江", "鹤岗", "101051201"),
			new CityCodeInfo(423, "黑龙江", "齐齐哈尔", "101050201"),
			new CityCodeInfo(424, "黑龙江", "大兴安岭", "101050701"),
			new CityCodeInfo(425, "吉林", "长春", "101060101"),
			new CityCodeInfo(426, "吉林", "延吉", "101060301"),
			new CityCodeInfo(427, "吉林", "四平", "101060401"),
			new CityCodeInfo(428, "吉林", "白山", "101060901"),
			new CityCodeInfo(429, "吉林", "白城", "101060601"),
			new CityCodeInfo(430, "吉林", "辽源", "101060701"),
			new CityCodeInfo(431, "吉林", "松原", "101060801"),
			new CityCodeInfo(432, "吉林", "吉林", "101060201"),
			new CityCodeInfo(433, "吉林", "通化", "101060501"),
			new CityCodeInfo(434, "辽宁", "沈阳", "101070101"),
			new CityCodeInfo(435, "辽宁", "鞍山", "101070301"),
			new CityCodeInfo(436, "辽宁", "抚顺", "101070401"),
			new CityCodeInfo(437, "辽宁", "本溪", "101070501"),
			new CityCodeInfo(438, "辽宁", "丹东", "101070601"),
			new CityCodeInfo(439, "辽宁", "葫芦岛", "101071401"),
			new CityCodeInfo(440, "辽宁", "营口", "101070801"),
			new CityCodeInfo(441, "辽宁", "阜新", "101070901"),
			new CityCodeInfo(442, "辽宁", "辽阳", "101071001"),
			new CityCodeInfo(443, "辽宁", "铁岭", "101071101"),
			new CityCodeInfo(444, "辽宁", "朝阳", "101071201"),
			new CityCodeInfo(445, "辽宁", "盘锦", "101071301"),
			new CityCodeInfo(446, "辽宁", "大连", "101070201"),
			new CityCodeInfo(447, "辽宁", "锦州", "101070701")
		);
		
		return cityCodes.stream()
			.collect(Collectors.toMap(
				CityCodeInfo::cityCode,
				info -> info
			));
	}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherResponse(
            @JsonProperty("status") Integer status,
            @JsonProperty("message") String message,
            @JsonProperty("cityInfo") CityInfo cityInfo,
            @JsonProperty("data") WeatherDataWrapper data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CityInfo(
            @JsonProperty("city") String city,
            @JsonProperty("citykey") String cityKey,
            @JsonProperty("parent") String parent,
            @JsonProperty("updateTime") String updateTime
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherDataWrapper(
            @JsonProperty("shidu") String humidity,
            @JsonProperty("pm25") Double pm25,
            @JsonProperty("pm10") Double pm10,
            @JsonProperty("quality") String quality,
            @JsonProperty("wendu") String temperature,
            @JsonProperty("ganmao") String healthTip,
            @JsonProperty("forecast") List<Forecast> forecast
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(
            @JsonProperty("date") String date,
            @JsonProperty("high") String highTemp,
            @JsonProperty("low") String lowTemp,
            @JsonProperty("ymd") String ymd,
            @JsonProperty("week") String week,
            @JsonProperty("fx") String windDirection,
            @JsonProperty("fl") String windLevel,
            @JsonProperty("type") String weatherType,
            @JsonProperty("notice") String notice
    ) {
    }

	public record CityCodeInfo(
		Integer id,
		String province,
		String city,
		String cityCode
	) {
	}

    /**
     * Get current weather for a Chinese city by city code
     *
     * @param cityCode The city code (e.g., 101010100 for Beijing, 101020100 for Shanghai)
     * @return Current weather information for the specified city
     * @throws RestClientException if the request fails
     */
    @Tool(description = "Get current weather for a Chinese city. Input is city code (e.g., 101010100 for Beijing)")
    public String getCurrentWeather(@ToolParam(description = "City code (e.g., 101010100 for Beijing, 101020100 for Shanghai)") String cityCode) {
		ResponseEntity<byte[]> responseEntity = restClient.get()
				.uri("{cityCode}", cityCode)
				.retrieve()
				.toEntity(byte[].class);

		byte[] body = responseEntity.getBody();
		if (body == null) {
			throw new RuntimeException("Empty response from weather API");
		}

		String response = new String(body, StandardCharsets.UTF_8);

        WeatherResponse weatherResponse;
        try {
            weatherResponse = objectMapper.readValue(response, WeatherResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse weather data: " + e.getMessage());
        }

		if (200 != weatherResponse.status()) {
			throw new RuntimeException("Weather API returned error: " + weatherResponse.message());
		}

        WeatherDataWrapper data = weatherResponse.data();
        CityInfo cityInfo = weatherResponse.cityInfo();

        return String.format("""
                        城市：%s (%s)
                        温度：%s°C
                        湿度：%s
                        空气质量：%s (PM2.5: %s)
                        风向：%s %s
                        温馨提示：%s
                        更新时间：%s
                        """,
                cityInfo.city(),
                cityInfo.parent(),
                data.temperature(),
                data.humidity(),
                data.quality(),
                data.pm25(),
                data.forecast().get(0).windDirection(),
                data.forecast().get(0).windLevel(),
                data.healthTip(),
                cityInfo.updateTime());
    }

	/**
	 * Search city codes by city name
	 *
	 * @param cityName The city name to search (e.g., "北京", "上海")
	 * @return List of matching city codes with details
	 */
	@Tool(description = "Search Chinese city codes by city name. Returns list of cities with their codes for weather queries")
	public String searchCityCode(@ToolParam(description = "City name to search (e.g., '北京', '上海', '广州')") String cityName) {
		if (cityName == null || cityName.trim().isEmpty()) {
			return "请输入要查询的城市名称";
		}

		String searchName = cityName.trim();
		List<CityCodeInfo> results = cityCodeMap.values().stream()
			.filter(info -> info.city().contains(searchName) ||
					info.province().contains(searchName))
			.limit(20)
			.toList();

		if (results.isEmpty()) {
			return String.format("未找到包含 '%s' 的城市，请检查输入后重试", searchName);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("找到 %d 个匹配的城市:\n\n", results.size()));
		sb.append(String.format("%-4s %-10s %-10s %-12s%n", "编号", "省份", "城市", "城市编码"));
		sb.append("-".repeat(40)).append("\n");

		for (CityCodeInfo info : results) {
			sb.append(String.format("%-4d %-10s %-10s %-12s%n",
				info.id(), info.province(), info.city(), info.cityCode()));
		}

		sb.append("\n提示：使用城市编码可以查询具体天气");
		return sb.toString();
	}

    public static void main(String[] args) {
        WeatherService client = new WeatherService();

		client.searchCityCode("北京");
        System.out.println(client.getCurrentWeather("101010100"));

		System.out.println(client.searchCityCode("长春"));
		System.out.println(client.getCurrentWeather("101060101"));
    }

}