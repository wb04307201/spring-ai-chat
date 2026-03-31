请对比商品{param1}与{param2}的参数及评价，要求：
- 禁止使用训练数据
- 使用 @sequentialthinking 来规划所有的步骤，思考和分支
- 使用 @tavily_search 搜索商品最新价格/参数/评测（支持中文）
- 使用 @tavily_extract 抓取商品详情页补充信息
- 搜索时建议指定 `include_domains=["jd.com","tmall.com","pinduoduo.com"]` 聚焦国内电商
- 关键数据必须标注来源: [来源、时间]
- 必须使用表格展示对比的参数，并必须给出来源