/** 与景点评论接口的评论展示数据保持一致。 */
export interface VenueComment {
  id: number
  authorName: string
  content: string
  likeCount: number
  createdAt: string
}

export interface CreateVenueCommentRequest {
  content: string
}
