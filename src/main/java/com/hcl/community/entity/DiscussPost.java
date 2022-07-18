package com.hcl.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * <p>
 * 
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="DiscussPost对象", description="")
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
public class DiscussPost implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Id
    private Integer id;

    @Field(type = FieldType.Integer)
    private Integer userId;
    // 互联网校招
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @ApiModelProperty(value = "0-普通; 1-置顶;")
    @Field(type = FieldType.Integer)
    private Integer type;

    @ApiModelProperty(value = "0-正常; 1-精华; 2-拉黑;")
    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private Integer commentCount;

    @Field(type = FieldType.Double)
    private Double score;


}
