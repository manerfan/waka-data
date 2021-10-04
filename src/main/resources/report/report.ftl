<!-- https://www.bootcdn.cn/ -->
<!-- https://v5.bootcss.com/ -->
<!-- https://echarts.apache.org/zh/index.html -->

<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>${title}</title>

    <meta name="viewport" content="width=device-width,initial-scale=1">

    <!-- bootstrap -->
    <link href="https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet">

    <style>
        @font-face {
            font-family: 'alibaba-puhui';
            src: url('https://wakadata.oss-cn-beijing.aliyuncs.com/assets/fonts/alibaba-sans/Alibaba-PuHuiTi-Bold.ttf'),
            url('https://wakadata.oss-cn-beijing.aliyuncs.com/assets/fonts/alibaba-sans/Alibaba-PuHuiTi-Heavy.ttf'),
            url('https://wakadata.oss-cn-beijing.aliyuncs.com/assets/fonts/alibaba-sans/Alibaba-PuHuiTi-Medium.ttf'),
            url('https://wakadata.oss-cn-beijing.aliyuncs.com/assets/fonts/alibaba-sans/Alibaba-PuHuiTi-Regular.ttf');
        }

        body {
            font-family: "alibaba-puhui", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", "Liberation Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
        }

        .desktop {
            width: 100%;
            height: 100vh;
            background-image: url("https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/bg.jpg");
            background-repeat: no-repeat;
            background-position: center;
            background-attachment: fixed;
            background-size: cover;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            min-height: 758px;
            min-width: 360px;
        }

        .home {
            width: 480px;
            height: 100%;
            padding: 5em 0;
        }

        .app {
            width: 100%;
            height: 100%;
            background-color: #cbf1f5;
            border-radius: 1em;
            padding: 4em 1em 2em;
        }

        .content {
            width: 100%;
            height: 100%;
            border-radius: 1em;
            overflow-y: scroll;
        }

        .content > .card {
            position: relative;
            color: ghostwhite;
            width: 100%;
            min-height: 840px;
            max-height: 960px;
            padding: 2.5em 2em;
            border: 0 !important;
            border-radius: 0 !important;
            background-repeat: no-repeat;
            background-position: center bottom;
            background-size: 100%;
        }

        .content > .card p {
            margin-bottom: .5em;
        }

        .content > .card .stress {
            font-size: 1.4em;
        }

        .content > .card .weight {
        }

        .content > .card .light {
            font-size: .9em;
            opacity: .6;
        }

        .content > .card.main {
            background-color: #195efd;
            text-shadow: 2px 2px 5px #393e46;
            background-image: url("https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/main/main.bg.png");
        }

        .content > .card.summary {
            background-color: #25c24d;
            background-image: url("https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/summary/summary.bg.png");
        }

        .content > .card.range {
            background-color: #1e62fe;
            background-image: url("https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/range/range.bg.png");
            height: 1260px;
            max-height: 1360px;
        }

        .content > .card.hard {
            background-color: #082786;
            background-image: url("https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/hard/hard.bg.png");
            height: 960px;
            max-height: 1060px;
        }

        .content .chart {
            width: 100vm;
        }

        .home-btn {
            position: absolute;
            top: 10px;
            left: 10px;
            z-index: 999;
        }

        .logo {
            width: 80%;
            margin-bottom: 3em;
        }

        /* github corner https://tholman.com/github-corners/ */

        .github-corner {
            position: absolute;
            top: 0;
            right: 0;
            z-index: 999;
        }

        .github-corner:hover .octo-arm {
            animation: octocat-wave 560ms ease-in-out
        }

        @keyframes octocat-wave {
            0%, 100% {
                transform: rotate(0)
            }
            20%, 60% {
                transform: rotate(-25deg)
            }
            40%, 80% {
                transform: rotate(10deg)
            }
        }

        /* 小屏样式 */
        @media only screen and (max-width: 500px) {
            .github-corner:hover .octo-arm {
                animation: none
            }

            .github-corner .octo-arm {
                animation: octocat-wave 560ms ease-in-out
            }

            .desktop {
                height: 100%;
            }

            .home {
                width: 100%;
                height: 100%;
                padding: 0;
            }

            .app {
                width: 100%;
                height: 100%;
                background-color: transparent;
                border-radius: 0;
                padding: 0;
            }

            .content {
                border-radius: 0;
            }

            .logo {
                width: 80%;
                margin: 3em 0;
            }
        }

        /* 打印样式 */
        @media print {
        }
    </style>
</head>
<body>
<a href="https://manerfan.github.io/waka-data/" type="button" class="home-btn btn btn-outline-light">HOME</a>
<a href="https://github.com/manerfan/waka-data" target="_blank"
   class="github-corner" aria-label="View source on GitHub">
    <svg width="80" height="80" viewBox="0 0 250 250"
         style="fill:#fff; color:#151513; position: absolute; top: 0; border: 0; right: 0;" aria-hidden="true">
        <path d="M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z"></path>
        <path d="M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2"
              fill="currentColor" style="transform-origin: 130px 106px;" class="octo-arm"></path>
        <path d="M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z"
              fill="currentColor" class="octo-body"></path>
    </svg>
</a>

<div class="desktop">
    <div class="home">
        <div class="app">
            <div class="content">
                <div class="card main">
                    <img src="https://wakadata.oss-cn-beijing.aliyuncs.com/assets/images/manerfan.com-logo.png"
                         class="logo rounded float-start" alt="manerfan.com"/>
                    <h1 style="font-weight: bold; text-align:center;">技术人数据统计</h1>
                    <h4 style="text-align: center; margin-top: 0.5em;">Waka Waki 『${grading.type}』</h4>
                    <h6 style="text-align: center; margin-top: 1.5em;">
                        <#if grading.name == "DAILY">
                            ${range.start}
                        <#else>
                            ${range.start} - ${range.end}
                        </#if>
                    </h6>
                </div>
                <div class="card summary">
                    <p>
                        <#if grading.name == "DAILY">
                            <span class="light">${grading.desc}，共投入</span><br/>
                        <#else>
                            <span class="light">${grading.desc}，平均每天投入</span><br/>
                        </#if>
                    </p>
                    <p>
                        <span class="stress">${averageDurationsOnWorkDays.hour}</span>
                        <span class="weight">小时</span>
                        <span class="stress">${averageDurationsOnWorkDays.minute}</span>
                        <span class="weight">分钟</span>
                    </p>
                    <p>
                        <#if mostEarlyDay??>
                            <span class="light">跨度从</span>
                            <span class="stress">${mostEarlyDay.time}</span>
                        </#if>
                        <#if mostLateDay??>
                            <span class="light">一直到</span>
                            <span class="stress">${mostLateDay.time}</span>
                        </#if>
                    </p>
                    <#if favoritePeriod??>
                        <p>
                            <span class="light">最喜欢在</span><br/>
                        </p>
                        <p>
                            <span class="stress">${favoritePeriod.from}</span>
                            <span class="light">到</span>
                            <span class="stress">${favoritePeriod.end}</span>
                            <span class="light">之间搬砖</span>
                        </p>
                    </#if>
                    <div id="summary" class="chart"
                         style="width: 110%;height: 240px; position: relative; top: -20px; left: -20px;"></div>
                </div>
                <div class="card range">
                    <p>
                        <span class="light">${grading.desc}，投入</span>
                    </p>
                    <p style="margin-top: .5em;">
                        <span class="light">语言占比</span>
                    </p>
                    <div id="language" class="chart"
                         style="width: 100%; height: 120px; position: relative; margin-top: 10px;"></div>
                    <p style="margin-top: 1.5em;">
                        <span class="light">工具占比</span>
                    </p>
                    <div id="editor" class="chart"
                         style="width: 100%; height: 120px; position: relative; margin-top: 10px;"></div>
                    <p style="margin-top: 1.5em;">
                        <span class="light">项目占比</span>
                    </p>
                    <div id="project" class="chart"
                         style="width: 100%; height: 120px; position: relative; margin-top: 10px;"></div>
                    <p style="margin-top: 1.5em;">
                        <span class="light">动作占比</span>
                    </p>
                    <div id="action" class="chart"
                         style="width: 100%; height: 120px; position: relative; margin-top: 10px;"></div>
                </div>
                <div class="card hard">
                    <p>
                        <span class="light">${grading.desc}</span><br/>
                    </p>
                    <#if grading.name == "DAILY">
                        <#if mostHardDay??>
                            <p>
                                <span class="light">共投入</span>
                                <span class="stress">${mostHardDay.hour}</span>
                                <span class="light">小时</span>
                                <span class="stress">${mostHardDay.minute}</span>
                                <span class="light">分钟</span>
                            </p>
                        </#if>
                        <p>
                            <#if mostEarlyDay??>
                                <span class="light">从</span>
                                <span class="stress">${mostEarlyDay.time}</span>
                            </#if>
                            <#if mostLateDay??>
                                <br/>
                                <span class="light">一直到</span>
                                <span class="stress">${mostLateDay.time}</span>
                            </#if>
                        </p>
                    <#else>
                        <#if mostHardDay??>
                            <p>
                                <span class="weight">${mostHardDay.date}</span>
                                <span class="light">最辛苦，共投入</span>
                                <span class="stress">${mostHardDay.hour}</span>
                                <span class="light">小时</span>
                                <span class="stress">${mostHardDay.minute}</span>
                                <span class="light">分钟</span>
                            </p>
                        </#if>
                        <#if mostEarlyDay??>
                            <p>
                                <span class="weight">${mostEarlyDay.date}</span>
                                <span class="light">工作最早，</span>
                                <span class="stress">${mostEarlyDay.time}</span>
                                <span class="light">便开始工作</span>
                            </p>
                        </#if>
                        <#if mostLateDay??>
                            <p>
                                <span class="weight">${mostLateDay.date}</span>
                                <span class="light">工作最晚，</span>
                                <span class="light">一直到</span>
                                <span class="stress">${mostLateDay.time}</span>
                            </p>
                        </#if>
                    </#if>
                    <p style="margin-top: 1em;">
                        <span class="light">截止到今天，每一天都在努力着</span><br/>
                    </p>
                    <div id="hard" class="chart"
                         style="height: 120px; position: relative; top: -20px;"></div>
                </div>
                <!-- <div class="card category"></div>-->
            </div>
        </div>
    </div>
</div>

<!-- bootstrap -->
<script src="https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/5.0.2/js/bootstrap.bundle.min.js"></script>
<!-- echarts -->
<script src="https://cdn.bootcdn.net/ajax/libs/echarts/5.1.2/echarts.min.js"></script>

<script type="text/javascript">
    const summaryDom = document.getElementById('summary');
    const summaryChart = echarts.init(summaryDom);

    const summaryOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        xAxis: {
            data: ${durations.dataAxis},
            min: 0,
            max: 289,
            axisLabel: {
                color: '#fff'
            },
            axisTick: {
                show: false
            },
            axisLine: {
                show: false
            }
        },
        yAxis: {
            min: 0,
            splitLine: {
                show: false
            },
            axisLine: {
                show: false
            },
            axisTick: {
                show: false
            },
            axisLabel: {
                show: false
            }
        },
        series: [
            {
                type: 'line',
                smooth: true,
                showSymbol: false,
                lineStyle: {
                    width: 0
                },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {offset: 0, color: '#e03e36'},
                        {offset: 0.5, color: '#ff7c38'},
                        {offset: 1, color: '#fecea8'}
                    ])
                },
                data: ${durations.data}
            }
        ]
    };

    summaryChart.setOption(summaryOption);
</script>

<script type="text/javascript">
    const languageDom = document.getElementById('language');
    const languageChart = echarts.init(languageDom);
    const languageOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        legend: {
            type: 'scroll',
            pageIconSize: 8,
            orient: 'vertical',
            left: 'right',
            top: 'center',
            textStyle: {
                color: '#fff'
            }
        },
        series: [
            {
                name: 'Radius Mode',
                type: 'pie',
                radius: [0, 60],
                center: ['20%', '50%'],
                //roseType: 'radius',
                itemStyle: {
                    normal: {
                        borderRadius: 5,
                        color: function (params) {
                            const colorList = ["#91cc75", "#f9c958", "#ee6666", "#73c0de", "#3aa272", "#fc8453", "#9a60b4"];
                            return colorList[params.dataIndex % 7];
                        }
                    }
                },
                label: {
                    show: false
                },
                emphasis: {
                    label: {
                        show: true
                    }
                },
                data: ${languages.data}
            }
        ]
    };

    languageChart.setOption(languageOption);
</script>

<script type="text/javascript">
    const editorDom = document.getElementById('editor');
    const editorChart = echarts.init(editorDom);
    const editorOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        legend: {
            type: 'scroll',
            pageIconSize: 8,
            orient: 'vertical',
            left: 'left',
            top: 'center',
            textStyle: {
                color: '#fff'
            }
        },
        series: [
            {
                name: 'Radius Mode',
                type: 'pie',
                radius: [0, 60],
                center: ['80%', '50%'],
                //roseType: 'radius',
                itemStyle: {
                    normal: {
                        borderRadius: 5,
                        color: function (params) {
                            const colorList = ["#91cc75", "#f9c958", "#ee6666", "#73c0de", "#3aa272", "#fc8453", "#9a60b4"];
                            return colorList[params.dataIndex % 7];
                        }
                    }
                },
                label: {
                    show: false
                },
                emphasis: {
                    label: {
                        show: true
                    }
                },
                data: ${editors.data}
            }
        ]
    };

    editorChart.setOption(editorOption);
</script>


<script type="text/javascript">
    const projectDom = document.getElementById('project');
    const projectChart = echarts.init(projectDom);
    const projectOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        legend: {
            type: 'scroll',
            pageIconSize: 8,
            orient: 'vertical',
            left: 'right',
            top: 'center',
            textStyle: {
                color: '#fff'
            }
        },
        series: [
            {
                name: 'Radius Mode',
                type: 'pie',
                radius: [0, 60],
                center: ['20%', '50%'],
                //roseType: 'radius',
                itemStyle: {
                    normal: {
                        borderRadius: 5,
                        color: function (params) {
                            const colorList = ["#91cc75", "#f9c958", "#ee6666", "#73c0de", "#3aa272", "#fc8453", "#9a60b4"];
                            return colorList[params.dataIndex % 7];
                        }
                    }
                },
                label: {
                    show: false
                },
                emphasis: {
                    label: {
                        show: true
                    }
                },
                data: ${projects.data}
            }
        ]
    };

    projectChart.setOption(projectOption);
</script>


<script type="text/javascript">
    const actionDom = document.getElementById('action');
    const actionChart = echarts.init(actionDom);
    const actionOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        legend: {
            type: 'scroll',
            pageIconSize: 8,
            orient: 'vertical',
            left: 'left',
            top: 'center',
            textStyle: {
                color: '#fff'
            }
        },
        series: [
            {
                name: 'Radius Mode',
                type: 'pie',
                radius: [0, 60],
                center: ['80%', '50%'],
                //roseType: 'radius',
                itemStyle: {
                    normal: {
                        borderRadius: 5,
                        color: function (params) {
                            const colorList = ["#91cc75", "#f9c958", "#ee6666", "#73c0de", "#3aa272", "#fc8453", "#9a60b4"];
                            return colorList[params.dataIndex % 7];
                        }
                    }
                },
                label: {
                    show: false
                },
                emphasis: {
                    label: {
                        show: true
                    }
                },
                data: ${categories.data}
            }
        ]
    };

    actionChart.setOption(actionOption);
</script>

<script type="text/javascript">
    const hardDom = document.getElementById('hard');
    const hardChart = echarts.init(hardDom);
    const hardOption = {
        aria: {
            enabled: true,
            decal: {
                show: true
            }
        },
        visualMap: {
            min: 0,
            max: ${contributions.max?c},
            type: 'continuous',
            show: false,
            inRange: {
                color: ['#fcfefe', '#005792'],
                opacity: 1
            }
        },
        calendar: {
            left: 0,
            right: 0,
            cellSize: ['auto', 'auto'],
            range: '${contributions.year?c}',
            itemStyle: {
                borderWidth: 0.5
            },
            dayLabel: {
                firstDay: 1,
                show: false
            },
            monthLabel: {
                nameMap: 'cn',
                margin: 5,
                fontSize: 8,
                color: '#fff'
            },
            yearLabel: {show: false}
        },
        series: {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: ${contributions.data}
        }
    };

    hardChart.setOption(hardOption);
</script>
</body>
</html>
